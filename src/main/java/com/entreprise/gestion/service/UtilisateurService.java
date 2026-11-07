package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.*;
import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.*;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_PAR_DEFAUT = "ROLE_GESTIONNAIRE";
    private static final int CODE_MAX = 20;

    private final UtilisateurRepository     utilisateurRepository;
    private final AgentRepository           agentRepository;
    private final RoleRepository            roleRepository;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final PasswordEncoder           passwordEncoder;
    private final AuditService              auditService;

    @Transactional(readOnly = true)
    public Page<UtilisateurDTO> findAll(Pageable pageable) {
        return utilisateurRepository.findByCompteSystemeFalse(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public UtilisateurDetailDTO findById(Long id) { return toDetail(visible(id)); }

    /** Aperçu de l'agent pour un matricule saisi : jamais d'erreur si l'agent n'existe pas (lien souple). */
    @Transactional(readOnly = true)
    public AgentLieDTO apercuAgent(String matricule) {
        String m = nettoyer(matricule);
        if (m == null) throw new BusinessException("MATRICULE_REQUIS", "Matricule requis.");
        return agentRepository.findById(m).map(AgentLieDTO::de).orElseGet(() -> AgentLieDTO.absent(m));
    }

    @Transactional
    public UtilisateurDetailDTO create(CreateUtilisateurRequest req, UserDetailsImpl actor) {
        if (utilisateurRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("EMAIL_DEJA_UTILISE", "Email déjà utilisé : " + req.getEmail());
        }
        String matricule = nettoyer(req.getMatriculeAgent());
        verifierLienLibre(matricule, null);

        Utilisateur u = Utilisateur.builder()
                .email(req.getEmail()).motDePasseHash(passwordEncoder.encode(req.getPassword()))
                .nom(nettoyer(req.getNom())).prenom(nettoyer(req.getPrenom())).matriculeAgent(matricule)
                .statut(StatutUtilisateur.ACTIF).compteSysteme(false).build();
        reprendreIdentiteDeLAgent(u);
        Utilisateur saved = utilisateurRepository.save(u);

        List<String> roles = req.getRoles() == null || req.getRoles().isEmpty() ? List.of(ROLE_PAR_DEFAUT) : req.getRoles();
        appliquerRoles(saved, roles, actor);
        appliquerPerimetre(saved, req.getPerimetre());

        auditService.logAvecEntite(actor.getUtilisateur(), "CREATE_USER",
                "Créé : " + saved.getEmail() + " — agent lié : " + libelleLien(saved.getMatriculeAgent())
                        + ", rôles " + roles + ", périmètre : " + resume(perimetreDe(saved))
                        + " — par " + actor.getNomComplet(), null);
        return toDetail(saved);
    }

    @Transactional
    public UtilisateurDetailDTO update(Long id, UpdateUtilisateurRequest req, UserDetailsImpl actor) {
        Utilisateur cible = visible(id);   // un compte système est « introuvable » côté API
        String ancienLien = cible.getMatriculeAgent();
        PerimetreDTO avant = perimetreDe(cible);
        String matricule = nettoyer(req.matriculeAgent());
        verifierLienLibre(matricule, cible.getId());

        cible.setNom(nettoyer(req.nom()));
        cible.setPrenom(nettoyer(req.prenom()));
        cible.setMatriculeAgent(matricule);
        reprendreIdentiteDeLAgent(cible);
        appliquerRoles(cible, req.roles(), actor);
        appliquerPerimetre(cible, req.perimetre());
        utilisateurRepository.save(cible);

        PerimetreDTO apres = perimetreDe(cible);
        auditService.logAvecEntite(actor.getUtilisateur(), "UPDATE_USER",
                "Modifié : " + cible.getEmail() + " — agent lié : " + libelleLien(ancienLien) + " → " + libelleLien(matricule)
                        + ", rôles " + req.roles()
                        + (avant.equals(apres) ? ", périmètre inchangé" : ", périmètre : " + resume(avant) + " → " + resume(apres))
                        + " — par " + actor.getNomComplet(), null);
        return toDetail(cible);
    }

    @Transactional
    public Utilisateur changeStatut(Long id, String statut, UserDetailsImpl actor) {
        Utilisateur cible = utilisateurRepository.findById(id)
                .orElseThrow(() -> new BusinessException("UTILISATEUR_INTROUVABLE", "Utilisateur introuvable : " + id));

        if (cible.isCompteSysteme()) {
            throw new BusinessException("COMPTE_PROTEGE", "Ce compte est protégé et ne peut pas être modifié.");
        }
        if (cible.getId().equals(actor.getId()) && !"ACTIF".equals(statut)) {
            throw new BusinessException("AUTO_SUSPENSION_INTERDITE",
                    "Vous ne pouvez pas suspendre ou désactiver votre propre compte.");
        }
        boolean cibleEstAdminActif = cible.getRoles().stream()
                .anyMatch(r -> ROLE_ADMIN.equals(r.getNomRole())) && cible.getStatut() == StatutUtilisateur.ACTIF;
        if (cibleEstAdminActif && !"ACTIF".equals(statut)
                && utilisateurRepository.countByRoleAdminActifExcludingSysteme() <= 1) {
            throw new BusinessException("DERNIER_ADMIN", "Impossible : ce compte est le dernier administrateur actif.");
        }
        StatutUtilisateur nouveauStatut;
        try { nouveauStatut = StatutUtilisateur.valueOf(statut); }
        catch (IllegalArgumentException e) {
            throw new BusinessException("STATUT_INVALIDE", "Statut invalide : " + statut);
        }
        String ancien = cible.getStatut().name();
        cible.setStatut(nouveauStatut);
        Utilisateur saved = utilisateurRepository.save(cible);
        auditService.logAvecEntite(actor.getUtilisateur(), "CHANGE_STATUT",
                cible.getEmail() + " : " + ancien + " → " + statut + " par " + actor.getNomComplet(), null);
        return saved;
    }

    // ── Règles du lien et des droits ────────────────────────────────────────

    /** Un agent n'a qu'un compte. L'existence de l'agent n'est PAS exigée (le compte peut précéder l'import). */
    private void verifierLienLibre(String matricule, Long idCompte) {
        if (matricule == null) return;
        utilisateurRepository.findByMatriculeAgent(matricule)
                .filter(autre -> !autre.getId().equals(idCompte))
                .ifPresent(autre -> { throw new BusinessException("AGENT_DEJA_LIE",
                        "Ce matricule est déjà lié au compte " + autre.getEmail() + "."); });
    }

    /** Copie (une fois) le nom et le prénom de l'agent si le compte n'en a aucun. Pas de synchronisation ensuite. */
    private void reprendreIdentiteDeLAgent(Utilisateur u) {
        if (u.getMatriculeAgent() == null || u.getNom() != null || u.getPrenom() != null) return;
        agentRepository.findById(u.getMatriculeAgent()).ifPresent(a -> {
            u.setNom(a.getNom());
            u.setPrenom(a.getPrenoms());
        });
    }

    private void appliquerRoles(Utilisateur cible, List<String> demandes, UserDetailsImpl actor) {
        Set<String> voulus = new LinkedHashSet<>(demandes);
        boolean etaitAdmin = cible.getRoles().stream().anyMatch(r -> ROLE_ADMIN.equals(r.getNomRole()));
        if (etaitAdmin && !voulus.contains(ROLE_ADMIN)) {
            if (cible.getId().equals(actor.getId())) {
                throw new BusinessException("AUTO_RETRAIT_ADMIN", "Vous ne pouvez pas retirer votre propre rôle d'administrateur.");
            }
            if (cible.getStatut() == StatutUtilisateur.ACTIF && utilisateurRepository.countByRoleAdminActifExcludingSysteme() <= 1) {
                throw new BusinessException("DERNIER_ADMIN", "Impossible : ce compte est le dernier administrateur actif.");
            }
        }
        cible.getUtilisateurRoles().removeIf(ur -> !voulus.contains(ur.getRole().getNomRole()));
        Set<String> presents = cible.getUtilisateurRoles().stream().map(ur -> ur.getRole().getNomRole()).collect(Collectors.toSet());
        for (String nom : voulus) {
            if (presents.contains(nom)) continue;
            Role role = roleRepository.findByNomRole(nom)
                    .orElseThrow(() -> new BusinessException("ROLE_INCONNU", "Rôle inconnu : " + nom));
            cible.getUtilisateurRoles().add(utilisateurRoleRepository.save(UtilisateurRole.builder().utilisateur(cible).role(role).build()));
        }
    }

    /**
     * Remplace le périmètre par différence : on retire ce qui n'est plus voulu et on n'ajoute que ce qui manque.
     * Supprimer puis recréer la même valeur dans la même transaction violerait la contrainte d'unicité.
     */
    private void appliquerPerimetre(Utilisateur u, PerimetreDTO demande) {
        Map<DimensionPerimetre, Set<String>> voulus = new EnumMap<>(DimensionPerimetre.class);
        voulus.put(DimensionPerimetre.MINISTERE, codes(demande == null ? null : demande.ministeres()));
        voulus.put(DimensionPerimetre.SOA, codes(demande == null ? null : demande.soas()));
        voulus.put(DimensionPerimetre.FIV, codes(demande == null ? null : demande.fivs()));

        u.getPerimetres().removeIf(p -> !voulus.get(p.getDimension()).contains(p.getCode()));
        for (var entree : voulus.entrySet()) {
            Set<String> presents = u.getPerimetres().stream().filter(p -> p.getDimension() == entree.getKey())
                    .map(PerimetreUtilisateur::getCode).collect(Collectors.toSet());
            for (String code : entree.getValue()) {
                if (!presents.contains(code)) {
                    u.getPerimetres().add(PerimetreUtilisateur.builder().utilisateur(u).dimension(entree.getKey()).code(code).build());
                }
            }
        }
    }

    private static Set<String> codes(List<String> brut) {
        Set<String> resultat = new TreeSet<>();
        if (brut == null) return resultat;
        for (String c : brut) {
            String n = nettoyer(c);
            if (n == null) continue;
            if (n.length() > CODE_MAX) {
                throw new BusinessException("CODE_PERIMETRE_INVALIDE", "Code trop long (" + CODE_MAX + " caractères max) : « " + n + " »");
            }
            resultat.add(n);
        }
        return resultat;
    }

    // ── Conversions ─────────────────────────────────────────────────────────

    private Utilisateur visible(Long id) {
        return utilisateurRepository.findByIdVisibleParApi(id)
                .orElseThrow(() -> new BusinessException("UTILISATEUR_INTROUVABLE", "Utilisateur introuvable : " + id));
    }

    private UtilisateurDTO toDTO(Utilisateur u) {
        return new UtilisateurDTO(u.getId(), u.getEmail(), u.getStatut().name(), u.getDateCreation(), u.getDateDerniereConnexion(),
                u.nomComplet(), u.getMatriculeAgent(), nomsRoles(u), perimetreDe(u));
    }

    private UtilisateurDetailDTO toDetail(Utilisateur u) {
        AgentLieDTO agent = u.getMatriculeAgent() == null ? null
                : agentRepository.findById(u.getMatriculeAgent()).map(AgentLieDTO::de)
                        .orElseGet(() -> AgentLieDTO.absent(u.getMatriculeAgent()));
        return new UtilisateurDetailDTO(u.getId(), u.getEmail(), u.getStatut().name(), u.getDateCreation(), u.getDateDerniereConnexion(),
                u.getNom(), u.getPrenom(), u.getMatriculeAgent(), nomsRoles(u), perimetreDe(u), agent);
    }

    private static List<String> nomsRoles(Utilisateur u) {
        return u.getRoles().stream().map(Role::getNomRole).sorted().toList();
    }

    private static PerimetreDTO perimetreDe(Utilisateur u) {
        Map<DimensionPerimetre, List<String>> parDimension = new EnumMap<>(DimensionPerimetre.class);
        for (DimensionPerimetre d : DimensionPerimetre.values()) parDimension.put(d, new ArrayList<>());
        u.getPerimetres().forEach(p -> parDimension.get(p.getDimension()).add(p.getCode()));
        parDimension.values().forEach(Collections::sort);
        return new PerimetreDTO(parDimension.get(DimensionPerimetre.MINISTERE), parDimension.get(DimensionPerimetre.SOA),
                parDimension.get(DimensionPerimetre.FIV));
    }

    private static String resume(PerimetreDTO p) {
        return "ministères " + liste(p.ministeres()) + ", SOA " + liste(p.soas()) + ", FIV " + liste(p.fivs());
    }
    private static String liste(List<String> l) { return l.isEmpty() ? "tous" : l.toString(); }
    private static String libelleLien(String matricule) { return matricule == null ? "aucun" : matricule; }
    private static String nettoyer(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
