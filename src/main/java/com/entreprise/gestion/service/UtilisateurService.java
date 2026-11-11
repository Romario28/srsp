package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.*;
import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.*;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.acces.ControleAcces;
import com.entreprise.gestion.service.acces.Perimetre;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CHEF = "ROLE_CHEF";
    private static final String ROLE_RH = "ROLE_RH";
    private static final String ROLE_AGENT = "ROLE_AGENT";
    private static final String ROLE_PAR_DEFAUT = ROLE_AGENT;
    private static final Set<String> PROFILS = Set.of(ROLE_ADMIN, ROLE_CHEF, ROLE_RH, ROLE_AGENT);
    private static final int CODE_MAX = 20;

    private final UtilisateurRepository     utilisateurRepository;
    private final AgentRepository           agentRepository;
    private final RoleRepository            roleRepository;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final PasswordEncoder           passwordEncoder;
    private final AuditService              auditService;
    private final ControleAcces             controleAcces;

    /** ADMIN : tous les comptes (hors système), paginés par la base. CHEF : les comptes RH et Agent de son périmètre. */
    @Transactional(readOnly = true)
    public Page<UtilisateurDTO> findAll(Pageable pageable, UserDetailsImpl acteur) {
        if (acteur.hasRole("ADMIN")) {
            return utilisateurRepository.findByCompteSystemeFalseAndStatutNot(StatutUtilisateur.EN_ATTENTE, pageable).map(this::toDTO);
        }
        List<Utilisateur> visibles = visiblesPourChef(acteur);
        int debut = (int) Math.min(pageable.getOffset(), visibles.size());
        int fin = Math.min(debut + pageable.getPageSize(), visibles.size());
        return new PageImpl<>(visibles.subList(debut, fin).stream().map(this::toDTO).toList(), pageable, visibles.size());
    }

    @Transactional(readOnly = true)
    public UtilisateurDetailDTO findById(Long id, UserDetailsImpl acteur) {
        Utilisateur cible = visible(id);
        if (!acteur.hasRole("ADMIN")
                && !estDansLePerimetre(controleAcces.perimetreDe(acteur), cible, m -> agentRepository.findById(m).orElse(null))) {
            throw new BusinessException("UTILISATEUR_INTROUVABLE", "Utilisateur introuvable : " + id);
        }
        return toDetail(cible);
    }

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
        List<String> roles = req.getRoles() == null || req.getRoles().isEmpty() ? List.of(ROLE_PAR_DEFAUT) : req.getRoles();
        String profil = validerProfil(roles);
        String matricule = nettoyer(req.getMatriculeAgent());
        exigerMatriculePourAgent(profil, matricule);
        verifierLienLibre(matricule, null);

        Utilisateur u = Utilisateur.builder()
                .email(req.getEmail()).motDePasseHash(passwordEncoder.encode(req.getPassword()))
                .nom(nettoyer(req.getNom())).prenom(nettoyer(req.getPrenom())).matriculeAgent(matricule)
                .agirSurSonDossier(ROLE_AGENT.equals(profil) && req.isAgirSurSonDossier())
                .statut(StatutUtilisateur.ACTIF).compteSysteme(false).build();
        reprendreIdentiteDeLAgent(u);
        Utilisateur saved = utilisateurRepository.save(u);

        appliquerRoles(saved, roles, actor);
        appliquerPerimetre(saved, ROLE_AGENT.equals(profil) ? null : req.getPerimetre());

        auditService.logAvecEntite(actor.getUtilisateur(), "CREATE_USER",
                "Créé : " + saved.getEmail() + " — agent lié : " + libelleLien(saved.getMatriculeAgent())
                        + ", profil " + profil + ", périmètre : " + resume(perimetreDe(saved))
                        + (ROLE_AGENT.equals(profil) ? ", droit d'agir sur son dossier : " + oui(saved.isAgirSurSonDossier()) : "")
                        + " — par " + actor.getNomComplet(), null);
        return toDetail(saved);
    }

    @Transactional
    public UtilisateurDetailDTO update(Long id, UpdateUtilisateurRequest req, UserDetailsImpl actor) {
        String profil = validerProfil(req.roles());
        Utilisateur cible = visible(id);
        String ancienLien = cible.getMatriculeAgent();
        PerimetreDTO avant = perimetreDe(cible);
        boolean droitAvant = cible.isAgirSurSonDossier();
        String matricule = nettoyer(req.matriculeAgent());
        exigerMatriculePourAgent(profil, matricule);
        verifierLienLibre(matricule, cible.getId());

        cible.setNom(nettoyer(req.nom()));
        cible.setPrenom(nettoyer(req.prenom()));
        cible.setMatriculeAgent(matricule);
        cible.setAgirSurSonDossier(ROLE_AGENT.equals(profil) && req.agirSurSonDossier());
        reprendreIdentiteDeLAgent(cible);
        appliquerRoles(cible, req.roles(), actor);
        appliquerPerimetre(cible, ROLE_AGENT.equals(profil) ? null : req.perimetre());
        utilisateurRepository.save(cible);

        PerimetreDTO apres = perimetreDe(cible);
        auditService.logAvecEntite(actor.getUtilisateur(), "UPDATE_USER",
                "Modifié : " + cible.getEmail() + " — agent lié : " + libelleLien(ancienLien) + " → " + libelleLien(matricule)
                        + ", profil " + profil
                        + (avant.equals(apres) ? ", périmètre inchangé" : ", périmètre : " + resume(avant) + " → " + resume(apres))
                        + (ROLE_AGENT.equals(profil) || droitAvant
                                ? ", droit d'agir sur son dossier : " + oui(droitAvant) + " → " + oui(cible.isAgirSurSonDossier()) : "")
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

    private static String validerProfil(List<String> demandes) {
        Set<String> distincts = new LinkedHashSet<>(demandes);
        if (distincts.size() != 1 || !PROFILS.contains(distincts.iterator().next())) {
            throw new BusinessException("PROFIL_INVALIDE", "Un compte a un seul profil : Administrateur, Chef, RH ou Agent.");
        }
        return distincts.iterator().next();
    }

    private static void exigerMatriculePourAgent(String profil, String matricule) {
        if (ROLE_AGENT.equals(profil) && matricule == null) {
            throw new BusinessException("AGENT_SANS_MATRICULE",
                    "Un compte Agent doit être lié à un matricule : c'est ainsi qu'il retrouve son dossier.");
        }
    }

    /** Comptes RH et Agent rattachés au périmètre du chef. Jamais : administrateurs, autres chefs, le chef lui-même, comptes système, demandes en attente. */
    private List<Utilisateur> visiblesPourChef(UserDetailsImpl chef) {
        Perimetre perimetre = controleAcces.perimetreDe(chef);
        if (perimetre.estVide()) return List.of();
        List<Utilisateur> candidats = utilisateurRepository.findAll(Sort.by("email")).stream()
                .filter(u -> !u.isCompteSysteme() && !u.getId().equals(chef.getId()) && u.getStatut() != StatutUtilisateur.EN_ATTENTE)
                .filter(u -> !aRole(u, ROLE_ADMIN) && !aRole(u, ROLE_CHEF) && (aRole(u, ROLE_RH) || aRole(u, ROLE_AGENT)))
                .toList();
        List<String> matricules = candidats.stream().filter(u -> !aRole(u, ROLE_RH)).map(Utilisateur::getMatriculeAgent)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, Agent> agents = matricules.isEmpty() ? Map.of()
                : agentRepository.findAllById(matricules).stream().collect(Collectors.toMap(Agent::getMatricule, a -> a));
        return candidats.stream().filter(u -> estDansLePerimetre(perimetre, u, agents::get)).toList();
    }

    /** Compte RH : son périmètre doit tenir dans celui du chef. Compte Agent : l'agent lié doit être dans le périmètre du chef. */
    private boolean estDansLePerimetre(Perimetre chef, Utilisateur u, Function<String, Agent> agentDe) {
        if (aRole(u, ROLE_ADMIN) || aRole(u, ROLE_CHEF) || u.isCompteSysteme() || u.getStatut() == StatutUtilisateur.EN_ATTENTE) return false;
        if (aRole(u, ROLE_RH)) return chef.couvre(perimetreCompte(u));
        if (aRole(u, ROLE_AGENT)) {
            Agent agent = u.getMatriculeAgent() == null ? null : agentDe.apply(u.getMatriculeAgent());
            return agent != null && chef.autorise(agent);
        }
        return false;
    }

    private static boolean aRole(Utilisateur u, String role) {
        return u.getRoles().stream().anyMatch(r -> role.equals(r.getNomRole()));
    }

    private static Perimetre perimetreCompte(Utilisateur u) {
        Set<String> ministeres = new HashSet<>(), soas = new HashSet<>(), fivs = new HashSet<>();
        for (PerimetreUtilisateur p : u.getPerimetres()) {
            switch (p.getDimension()) {
                case MINISTERE -> ministeres.add(p.getCode());
                case SOA -> soas.add(p.getCode());
                case FIV -> fivs.add(p.getCode());
            }
        }
        return new Perimetre(false, ministeres, soas, fivs);
    }

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

    private Utilisateur visible(Long id) {
        return utilisateurRepository.findByIdVisibleParApi(id)
                .orElseThrow(() -> new BusinessException("UTILISATEUR_INTROUVABLE", "Utilisateur introuvable : " + id));
    }

    private UtilisateurDTO toDTO(Utilisateur u) {
        return new UtilisateurDTO(u.getId(), u.getEmail(), u.getStatut().name(), u.getDateCreation(), u.getDateDerniereConnexion(),
                u.nomComplet(), u.getMatriculeAgent(), nomsRoles(u), perimetreDe(u), u.isAgirSurSonDossier());
    }

    private UtilisateurDetailDTO toDetail(Utilisateur u) {
        AgentLieDTO agent = u.getMatriculeAgent() == null ? null
                : agentRepository.findById(u.getMatriculeAgent()).map(AgentLieDTO::de)
                        .orElseGet(() -> AgentLieDTO.absent(u.getMatriculeAgent()));
        return new UtilisateurDetailDTO(u.getId(), u.getEmail(), u.getStatut().name(), u.getDateCreation(), u.getDateDerniereConnexion(),
                u.getNom(), u.getPrenom(), u.getMatriculeAgent(), nomsRoles(u), perimetreDe(u), agent,
                u.isAgirSurSonDossier());
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
    private static String oui(boolean valeur) { return valeur ? "oui" : "non"; }
    private static String nettoyer(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
