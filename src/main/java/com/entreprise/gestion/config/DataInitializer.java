package com.entreprise.gestion.config;

import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Rôles : créés s'ils manquent (toute base). Jeu de démonstration : base sans compte uniquement.
 *   admin@entreprise.mg            ADMIN : accès total
 *   sophie.martin@entreprise.mg    CHEF : ministère 05 ; voit aussi la liste des comptes de son périmètre
 *   hanta.rabe@entreprise.mg       RH : ministère 05 + localité L01
 *   jean.rakoto@entreprise.mg      AGENT lié au matricule 123456, lecture seule
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
    private final DepartementRepository           departementRepository;   // [NOUVEAU]
    private final PasswordEncoder                 passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        Permission pRead    = permission("ANTICIPATION_READ", "Consulter l'anticipation");
        Permission pDossier = permission("DOSSIER_MANAGE", "Démarrer / classer / rouvrir un dossier");
        Permission pUser    = permission("USER_MANAGE", "Gérer les comptes");
        Permission pUserRead = permission("USER_READ", "Lister les comptes de son périmètre");
        Permission pAudit   = permission("AUDIT_READ", "Consulter l'audit");

        Role roleAdmin = role("ROLE_ADMIN", "Accès complet", pRead, pDossier, pUser, pAudit);
        Role roleChef  = role("ROLE_CHEF", "Chef / Supérieur : anticipation et comptes de son périmètre", pRead, pDossier, pUserRead);
        Role roleRh    = role("ROLE_RH", "Utilisateur RH : anticipation dans son périmètre", pRead, pDossier);
        Role roleAgent = role("ROLE_AGENT", "Agent : son propre espace", pRead);

        if (utilisateurRepository.count() > 0) return;

        // [NOUVEAU] démonstration seulement : sans département, la page publique de demande ne peut pas être envoyée
        if (departementRepository.count() == 0) {
            departementRepository.save(Departement.builder()
                    .libelle("(SRSP) Service Regional de la Solde et des Pensions").build());
        }

        Utilisateur admin = compte("admin@entreprise.mg", "Admin123!", null, null, null);
        assignRole(admin, roleAdmin);

        Utilisateur sophie = compte("sophie.martin@entreprise.mg", "Chef123!", "Sophie", "Martin", null);
        assignRole(sophie, roleChef);
        perimetre(sophie, DimensionPerimetre.MINISTERE, "05");

        Utilisateur hanta = compte("hanta.rabe@entreprise.mg", "Rh123!", "Hanta", "Rabe", null);
        assignRole(hanta, roleRh);
        perimetre(hanta, DimensionPerimetre.MINISTERE, "05");
        perimetre(hanta, DimensionPerimetre.FIV, "L01");

        Utilisateur jean = compte("jean.rakoto@entreprise.mg", "Agent123!", "Jean", "Rakoto", "123456");
        assignRole(jean, roleAgent);

        String motDePasseSecours = System.getenv().getOrDefault("SUPERADMIN_PASSWORD", "SuperSecret_ChangeMe_2024!");
        Utilisateur superAdmin = utilisateurRepository.save(Utilisateur.builder()
                .email("superadmin@entreprise.mg").motDePasseHash(passwordEncoder.encode(motDePasseSecours))
                .statut(StatutUtilisateur.ACTIF).compteSysteme(true).build());
        assignRole(superAdmin, roleAdmin);

        System.out.println("""

            ╔════════════════════════════════════════════════════════════════╗
            ║ admin@entreprise.mg           Admin123!   ADMIN                 ║
            ║ sophie.martin@entreprise.mg   Chef123!    CHEF (ministère 05)   ║
            ║ hanta.rabe@entreprise.mg      Rh123!      RH (05 + L01)         ║
            ║ jean.rakoto@entreprise.mg     Agent123!   AGENT (123456)        ║
            ╚════════════════════════════════════════════════════════════════╝
            """);
    }

    private Permission permission(String nom, String desc) {
        return permissionRepository.findByNomPermission(nom)
                .orElseGet(() -> permissionRepository.save(Permission.builder().nomPermission(nom).description(desc).build()));
    }

    private Role role(String nom, String desc, Permission... permissions) {
        return roleRepository.findByNomRole(nom).orElseGet(() -> {
            Role r = Role.builder().nomRole(nom).description(desc).build();
            r.setPermissions(new HashSet<>(Set.of(permissions)));
            return roleRepository.save(r);
        });
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
