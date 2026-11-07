package com.entreprise.gestion.config;

import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * Jeu de démonstration (base vide uniquement) :
 *   admin@entreprise.mg            ADMIN : accès total
 *   jean.rakoto@entreprise.mg      gestionnaire lié au matricule 123456, périmètre : ministère 05
 *   hanta.rabe@entreprise.mg       gestionnaire sans lien agent, périmètre : ministère 05 + localité L01
 * Les codes de périmètre sont des textes libres : ils prendront effet une fois les référentiels importés.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository                  roleRepository;
    private final PermissionRepository            permissionRepository;
    private final UtilisateurRepository           utilisateurRepository;
    private final UtilisateurRoleRepository       utilisateurRoleRepository;
    private final PerimetreUtilisateurRepository  perimetreRepository;
    private final PasswordEncoder                 passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        if (utilisateurRepository.count() > 0) return;   // [NOUVEAU] base déjà initialisée (profil persistant)

        Permission pRead   = permissionRepository.save(perm("ANTICIPATION_READ", "Consulter l'anticipation"));
        Permission pDossier = permissionRepository.save(perm("DOSSIER_MANAGE", "Démarrer / classer / rouvrir un dossier"));
        Permission pUser   = permissionRepository.save(perm("USER_MANAGE", "Gérer les comptes"));
        Permission pAudit  = permissionRepository.save(perm("AUDIT_READ", "Consulter l'audit"));

        Role roleGestionnaire = roleRepository.save(role("ROLE_GESTIONNAIRE", "Authentifié : périmètre défini par ses ministères, SOA et localités", pRead, pDossier));
        Role roleAdmin        = roleRepository.save(role("ROLE_ADMIN", "Accès complet", pRead, pDossier, pUser, pAudit));

        Utilisateur admin = compte("admin@entreprise.mg", "Admin123!", null, null, null);
        assignRole(admin, roleAdmin);

        Utilisateur jean = compte("jean.rakoto@entreprise.mg", "Gest123!", "Jean", "Rakoto", "123456");
        assignRole(jean, roleGestionnaire);
        perimetre(jean, DimensionPerimetre.MINISTERE, "05");

        Utilisateur hanta = compte("hanta.rabe@entreprise.mg", "Gest123!", "Hanta", "Rabe", null);
        assignRole(hanta, roleGestionnaire);
        perimetre(hanta, DimensionPerimetre.MINISTERE, "05");
        perimetre(hanta, DimensionPerimetre.FIV, "L01");

        String motDePasseSecours = System.getenv().getOrDefault("SUPERADMIN_PASSWORD", "SuperSecret_ChangeMe_2024!");
        Utilisateur superAdmin = utilisateurRepository.save(Utilisateur.builder()
                .email("superadmin@entreprise.mg").motDePasseHash(passwordEncoder.encode(motDePasseSecours))
                .statut(StatutUtilisateur.ACTIF).compteSysteme(true).build());
        assignRole(superAdmin, roleAdmin);

        System.out.println("""

            ╔═══════════════════════════════════════════════════════════════╗
            ║ admin@entreprise.mg           Admin123!  ADMIN                 ║
            ║ jean.rakoto@entreprise.mg     Gest123!   gestionnaire (123456) ║
            ║ hanta.rabe@entreprise.mg      Gest123!   gestionnaire          ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);
    }

    private Permission perm(String nom, String desc) {
        return Permission.builder().nomPermission(nom).description(desc).build();
    }

    private Role role(String nom, String desc, Permission... permissions) {
        Role r = Role.builder().nomRole(nom).description(desc).build();
        r.setPermissions(Set.of(permissions));
        return r;
    }

    private Utilisateur compte(String email, String motDePasse, String prenom, String nom, String matricule) {
        return utilisateurRepository.save(Utilisateur.builder()
                .email(email).motDePasseHash(passwordEncoder.encode(motDePasse))
                .prenom(prenom).nom(nom).matriculeAgent(matricule)
                .statut(StatutUtilisateur.ACTIF).compteSysteme(false).build());
    }

    private void assignRole(Utilisateur u, Role r) {
        utilisateurRoleRepository.save(UtilisateurRole.builder().utilisateur(u).role(r).build());
    }

    private void perimetre(Utilisateur u, DimensionPerimetre dimension, String code) {
        perimetreRepository.save(PerimetreUtilisateur.builder().utilisateur(u).dimension(dimension).code(code).build());
    }
}
