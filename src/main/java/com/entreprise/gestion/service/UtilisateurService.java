package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.CreateUtilisateurRequest;
import com.entreprise.gestion.dto.UtilisateurDTO;
import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.*;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository     utilisateurRepository;
    private final EmployeRepository         employeRepository;
    private final RoleRepository            roleRepository;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final PasswordEncoder           passwordEncoder;
    private final AuditService              auditService;

    @Transactional(readOnly = true)
    public Page<UtilisateurDTO> findAll(Pageable pageable) {
        return utilisateurRepository.findByCompteSystemeFalse(pageable).map(this::toDTO);
    }

    @Transactional(readOnly = true)
    public UtilisateurDTO findById(Long id) {
        Utilisateur u = utilisateurRepository.findByIdVisibleParApi(id)
                .orElseThrow(() -> new BusinessException("UTILISATEUR_INTROUVABLE", "Utilisateur introuvable : " + id));
        return toDTO(u);
    }

    @Transactional
    public Utilisateur create(CreateUtilisateurRequest req, UserDetailsImpl actor) {
        if (utilisateurRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("EMAIL_DEJA_UTILISE", "Email déjà utilisé : " + req.getEmail());
        }

        Utilisateur utilisateur = Utilisateur.builder()
                .email(req.getEmail()).motDePasseHash(passwordEncoder.encode(req.getPassword()))
                .statut(StatutUtilisateur.ACTIF).compteSysteme(false).build();

        if (req.getIdEmploye() != null) {
            Employe employe = employeRepository.findById(req.getIdEmploye())
                    .orElseThrow(() -> new BusinessException("EMPLOYE_INTROUVABLE",
                            "Employé introuvable : " + req.getIdEmploye()));
            if (employe.getUtilisateur() != null) {
                throw new BusinessException("EMPLOYE_DEJA_LIE", "Cet employé possède déjà un compte.");
            }
            utilisateur.setEmploye(employe);
        }

        Utilisateur saved = utilisateurRepository.save(utilisateur);

        List<String> rolesNames = (req.getRoles() != null && !req.getRoles().isEmpty())
                ? req.getRoles() : List.of("ROLE_EMPLOYE");

        for (String nomRole : rolesNames) {
            Role role = roleRepository.findByNomRole(nomRole)
                    .orElseThrow(() -> new BusinessException("ROLE_INCONNU", "Rôle inconnu : " + nomRole));
            utilisateurRoleRepository.save(UtilisateurRole.builder().utilisateur(saved).role(role).build());
        }

        auditService.logAvecEntite(actor.getUtilisateur(), "CREATE_USER",
                "Créé : " + saved.getEmail() + " par " + actor.getNomComplet(), null);

        return saved;
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
                .anyMatch(r -> "ROLE_ADMIN".equals(r.getNomRole())) && cible.getStatut() == StatutUtilisateur.ACTIF;

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

    private UtilisateurDTO toDTO(Utilisateur u) {
        UtilisateurDTO dto = new UtilisateurDTO();
        dto.setId(u.getId());
        dto.setEmail(u.getEmail());
        dto.setStatut(u.getStatut().name());
        dto.setDateCreation(u.getDateCreation());
        dto.setDateDerniereConnexion(u.getDateDerniereConnexion());
        if (u.getEmploye() != null) {
            dto.setNomEmploye(u.getEmploye().getPrenom() + " " + u.getEmploye().getNom());
            dto.setMatriculeEmploye(u.getEmploye().getMatricule());
            if (u.getEmploye().getDepartement() != null) {
                dto.setNomDepartement(u.getEmploye().getDepartement().getNomDepartement());
            }
        }
        dto.setRoles(u.getRoles().stream().map(Role::getNomRole).collect(Collectors.toList()));
        return dto;
    }
}