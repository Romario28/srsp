package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.AgentLieDTO;
import com.entreprise.gestion.dto.ApprobationRequest;
import com.entreprise.gestion.dto.DemandeCompteDTO;
import com.entreprise.gestion.dto.DemandeCompteRequest;
import com.entreprise.gestion.dto.PerimetreDTO;
import com.entreprise.gestion.dto.RefusRequest;
import com.entreprise.gestion.entite.DimensionPerimetre;
import com.entreprise.gestion.entite.PerimetreUtilisateur;
import com.entreprise.gestion.entite.Role;
import com.entreprise.gestion.entite.StatutUtilisateur;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.entite.UtilisateurRole;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.RoleRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.repository.UtilisateurRoleRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.acces.ControleAcces;
import com.entreprise.gestion.service.acces.Perimetre;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Inscription demandée par l'utilisateur, validée par un administrateur ou un chef.
 * Une demande est un compte au statut EN_ATTENTE : il porte déjà son profil et son mot de passe haché.
 */
@Service
@RequiredArgsConstructor
public class DemandeCompteService {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String ROLE_CHEF = "ROLE_CHEF";
    private static final String ROLE_RH = "ROLE_RH";
    private static final String ROLE_AGENT = "ROLE_AGENT";
    private static final Set<String> PROFILS_AUTORISES = Set.of(ROLE_AGENT, ROLE_RH, ROLE_CHEF);
    private static final int CODE_MAX = 20;

    private final UtilisateurRepository     utilisateurRepository;
    private final AgentRepository           agentRepository;
    private final RoleRepository            roleRepository;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final PasswordEncoder           passwordEncoder;
    private final AuditService              auditService;
    private final ControleAcces             controleAcces;

    @Value("${comptes.demandes-max-en-attente:200}")
    private int maxDemandesEnAttente;

    @Transactional
    public void soumettre(DemandeCompteRequest req, String ip) {
        if (utilisateurRepository.countByStatut(StatutUtilisateur.EN_ATTENTE) >= maxDemandesEnAttente) {
            throw new BusinessException("PLAFOND_DEMANDES", "Le nombre maximum de demandes en attente est atteint.");
        }

        String profil = req.profil() != null ? req.profil().trim() : "";
        if (!PROFILS_AUTORISES.contains(profil)) {
            throw new BusinessException("PROFIL_INTERDIT", "Le profil demandé n'est pas autorisé.");
        }

        String matricule = nettoyer(req.matricule());
        if (matricule == null) {
            throw new BusinessException("MATRICULE_REQUIS", "Le matricule est obligatoire.");
        }

        String email = nettoyer(req.email());
        if (email == null) {
            throw new BusinessException("EMAIL_REQUIS", "L'e-mail est obligatoire.");
        }

        // Unicité : message générique pour ne pas révéler si c'est l'e-mail ou le matricule qui est déjà utilisé
        if (utilisateurRepository.existsByEmailIgnoreCase(email) || utilisateurRepository.findByMatriculeAgent(matricule).isPresent()) {
            throw new BusinessException("DEMANDE_IMPOSSIBLE", "Impossible d'enregistrer la demande avec ces informations.");
        }

        Utilisateur u = Utilisateur.builder()
                .email(email.toLowerCase())
                .motDePasseHash(passwordEncoder.encode(req.password()))
                .nom(nettoyer(req.nom()))
                .prenom(nettoyer(req.prenom()))
                .matriculeAgent(matricule)
                .statut(StatutUtilisateur.EN_ATTENTE)
                .compteSysteme(false)
                .agirSurSonDossier(false)
                .build();
        Utilisateur saved = utilisateurRepository.save(u);

        Role role = roleRepository.findByNomRole(profil)
                .orElseThrow(() -> new BusinessException("ROLE_INCONNU", "Rôle inconnu : " + profil));
        UtilisateurRole ur = UtilisateurRole.builder().utilisateur(saved).role(role).build();
        utilisateurRoleRepository.save(ur);
        saved.getUtilisateurRoles().add(ur);

        auditService.logAnonyme("Système", "DEMANDE_COMPTE",
                "Demande de compte pour " + saved.getEmail() + " (profil " + profil + ", matricule " + matricule + ")", ip);
    }

    @Transactional(readOnly = true)
    public List<DemandeCompteDTO> lister(UserDetailsImpl acteur) {
        List<Utilisateur> enAttente = utilisateurRepository.findByStatutOrderByDateCreationAsc(StatutUtilisateur.EN_ATTENTE);
        if (acteur.hasRole("ADMIN")) {
            return enAttente.stream().map(this::toDTO).toList();
        }

        Perimetre perimetreChef = controleAcces.perimetreDe(acteur);
        if (perimetreChef.estVide()) return List.of();

        List<String> matricules = enAttente.stream().map(Utilisateur::getMatriculeAgent)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, Agent> agents = matricules.isEmpty() ? Map.of()
                : agentRepository.findAllById(matricules).stream().collect(Collectors.toMap(Agent::getMatricule, a -> a));

        return enAttente.stream()
                .filter(u -> visiblePourChef(u, perimetreChef, agents::get))
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public long compter(UserDetailsImpl acteur) {
        if (acteur.hasRole("ADMIN")) {
            return utilisateurRepository.countByStatut(StatutUtilisateur.EN_ATTENTE);
        }
        return lister(acteur).size();
    }

    @Transactional
    public void approuver(Long id, ApprobationRequest req, UserDetailsImpl acteur) {
        Utilisateur cible = utilisateurRepository.findById(id)
                .orElseThrow(() -> new BusinessException("DEMANDE_INTROUVABLE", "Demande introuvable : " + id));
        if (cible.getStatut() != StatutUtilisateur.EN_ATTENTE) {
            throw new BusinessException("DEMANDE_INTROUVABLE", "Demande introuvable : " + id);
        }

        String profil = profilDe(cible);
        boolean admin = acteur.hasRole("ADMIN");

        if (!admin) {
            Perimetre perimetreChef = controleAcces.perimetreDe(acteur);
            Agent agent = cible.getMatriculeAgent() == null ? null : agentRepository.findById(cible.getMatriculeAgent()).orElse(null);
            if (!visiblePourChef(cible, perimetreChef, m -> agent)) {
                throw new BusinessException("DEMANDE_INTROUVABLE", "Demande introuvable : " + id);
            }
            if (!ROLE_AGENT.equals(profil)) {
                Perimetre demandePerimetre = perimetreDepuis(req != null ? req.perimetre() : null);
                if (demandePerimetre.estVide()) {
                    throw new BusinessException("PERIMETRE_REQUIS", "Vous devez accorder au moins une valeur de périmètre pour pouvoir approuver.");
                }
                if (!perimetreChef.couvre(demandePerimetre)) {
                    throw new BusinessException("PERIMETRE_HORS_LIMITES", "Le périmètre accordé doit être contenu dans le vôtre.");
                }
            }
        }

        if (ROLE_AGENT.equals(profil)) {
            cible.getPerimetres().clear();
        } else {
            appliquerPerimetre(cible, req != null ? req.perimetre() : null);
        }

        cible.setStatut(StatutUtilisateur.ACTIF);
        utilisateurRepository.save(cible);

        auditService.logAvecEntite(acteur.getUtilisateur(), "APPROBATION_COMPTE",
                "Compte activé pour " + cible.getEmail() + " (profil " + profil + ", matricule " + cible.getMatriculeAgent() + ") par " + acteur.getNomComplet(), null);
    }

    @Transactional
    public void refuser(Long id, RefusRequest req, UserDetailsImpl acteur) {
        Utilisateur cible = utilisateurRepository.findById(id)
                .orElseThrow(() -> new BusinessException("DEMANDE_INTROUVABLE", "Demande introuvable : " + id));
        if (cible.getStatut() != StatutUtilisateur.EN_ATTENTE) {
            throw new BusinessException("DEMANDE_INTROUVABLE", "Demande introuvable : " + id);
        }

        if (!acteur.hasRole("ADMIN")) {
            Perimetre perimetreChef = controleAcces.perimetreDe(acteur);
            Agent agent = cible.getMatriculeAgent() == null ? null : agentRepository.findById(cible.getMatriculeAgent()).orElse(null);
            if (!visiblePourChef(cible, perimetreChef, m -> agent)) {
                throw new BusinessException("DEMANDE_INTROUVABLE", "Demande introuvable : " + id);
            }
        }

        String email = cible.getEmail();
        String matricule = cible.getMatriculeAgent();
        String motif = req != null && req.motif() != null ? req.motif().trim() : "";

        utilisateurRepository.delete(cible);

        auditService.logAvecEntite(acteur.getUtilisateur(), "REFUS_COMPTE",
                "Demande de compte refusée pour " + email + " (matricule " + matricule + ") par " + acteur.getNomComplet()
                        + (motif.isEmpty() ? "" : " — Motif : " + motif), null);
    }

    private boolean visiblePourChef(Utilisateur u, Perimetre chef, java.util.function.Function<String, Agent> agentDe) {
        String profil = profilDe(u);
        if (ROLE_ADMIN.equals(profil) || ROLE_CHEF.equals(profil)) return false;
        if (!ROLE_RH.equals(profil) && !ROLE_AGENT.equals(profil)) return false;
        if (u.getMatriculeAgent() == null) return false;
        Agent a = agentDe.apply(u.getMatriculeAgent());
        return a != null && chef.autorise(a);
    }

    private DemandeCompteDTO toDTO(Utilisateur u) {
        String matricule = u.getMatriculeAgent();
        Agent agent = matricule == null ? null : agentRepository.findById(matricule).orElse(null);
        AgentLieDTO agentLie = agent != null ? AgentLieDTO.de(agent) : AgentLieDTO.absent(matricule != null ? matricule : "");
        PerimetreDTO perimetreSuggere = perimetreSuggere(agent);
        return new DemandeCompteDTO(u.getId(), u.getEmail(), u.getNom(), u.getPrenom(), matricule,
                profilDe(u), u.getDateCreation(), agentLie, perimetreSuggere);
    }

    private static String profilDe(Utilisateur u) {
        return u.getRoles().stream().map(Role::getNomRole).findFirst().orElse("");
    }

    private static PerimetreDTO perimetreSuggere(Agent agent) {
        if (agent == null) return new PerimetreDTO(List.of(), List.of(), List.of());
        List<String> mins = agent.getMinistere() != null && agent.getMinistere().getCode() != null
                ? List.of(agent.getMinistere().getCode()) : List.of();
        List<String> soas = agent.getSoa() != null && agent.getSoa().getCode() != null
                ? List.of(agent.getSoa().getCode()) : List.of();
        List<String> fivs = agent.getLocalite() != null && agent.getLocalite().getCode() != null
                ? List.of(agent.getLocalite().getCode()) : List.of();
        return new PerimetreDTO(mins, soas, fivs);
    }

    private static Perimetre perimetreDepuis(PerimetreDTO dto) {
        if (dto == null) return Perimetre.aucun();
        Set<String> mins = dto.ministeres() == null ? Set.of() : dto.ministeres().stream().map(DemandeCompteService::nettoyer).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> soas = dto.soas() == null ? Set.of() : dto.soas().stream().map(DemandeCompteService::nettoyer).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<String> fivs = dto.fivs() == null ? Set.of() : dto.fivs().stream().map(DemandeCompteService::nettoyer).filter(Objects::nonNull).collect(Collectors.toSet());
        return new Perimetre(false, mins, soas, fivs);
    }

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

    private static String nettoyer(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
