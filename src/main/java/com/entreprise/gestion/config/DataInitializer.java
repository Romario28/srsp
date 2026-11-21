package com.entreprise.gestion.config;

import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Rôles : créés s'ils manquent (toute base). Jeu de démonstration : base sans compte uniquement.
 *   admin@entreprise.mg            ADMIN : accès total
 *   sophie.martin@entreprise.mg    CHEF : ministère 05 ; voit aussi la liste des comptes de son périmètre
 *   hanta.rabe@entreprise.mg       RH : ministère 05 + localité L01
 *   jean.rakoto@entreprise.mg      AGENT lié au matricule 123456, lecture seule
 * Les codes de périmètre sont des textes libres : ils prendront effet une fois les référentiels importés.
 * [MODIFIÉ] plus de permissions ; chaque compte reçoit son unique rôle à la création.
 */
@Component
@Order(1)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository                  roleRepository;
    // [SUPPRIMÉ] private final PermissionRepository            permissionRepository;
    private final UtilisateurRepository           utilisateurRepository;
    // [SUPPRIMÉ] private final UtilisateurRoleRepository       utilisateurRoleRepository;
    private final PerimetreUtilisateurRepository  perimetreRepository;
    private final DepartementRepository           departementRepository;
    private final PasswordEncoder                 passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {
        // [SUPPRIMÉ] les cinq permission(...) (ANTICIPATION_READ, DOSSIER_MANAGE, USER_MANAGE, USER_READ, AUDIT_READ)
        Role roleAdmin = role("ROLE_ADMIN", "Accès complet");
        Role roleChef  = role("ROLE_CHEF", "Chef / Supérieur : anticipation et comptes de son périmètre");
        Role roleRh    = role("ROLE_RH", "Utilisateur RH : anticipation dans son périmètre");
        Role roleAgent = role("ROLE_AGENT", "Agent : son propre espace");

        if (utilisateurRepository.count() > 0) return;

        // démonstration seulement : sans département, la page publique de demande ne peut pas être envoyée
        if (departementRepository.count() == 0) {
            departementRepository.save(Departement.builder()
                    .libelle("(SRSP) Service Regional de la Solde et des Pensions").build());
        }

        Utilisateur admin = compte("admin@entreprise.mg", "Admin123!", null, null, null, roleAdmin);

        Utilisateur sophie = compte("sophie.martin@entreprise.mg", "Chef123!", "Sophie", "Martin", null, roleChef);
        perimetre(sophie, DimensionPerimetre.MINISTERE, "05");

        Utilisateur hanta = compte("hanta.rabe@entreprise.mg", "Rh123!", "Hanta", "Rabe", null, roleRh);
        perimetre(hanta, DimensionPerimetre.MINISTERE, "05");
        perimetre(hanta, DimensionPerimetre.FIV, "L01");

        compte("jean.rakoto@entreprise.mg", "Agent123!", "Jean", "Rakoto", "123456", roleAgent);

        String motDePasseSecours = System.getenv().getOrDefault("SUPERADMIN_PASSWORD", "SuperSecret_ChangeMe_2024!");
        utilisateurRepository.save(Utilisateur.builder()
                .email("superadmin@entreprise.mg").motDePasseHash(passwordEncoder.encode(motDePasseSecours))
                .role(roleAdmin)   // [NOUVEAU]
                .statut(StatutUtilisateur.ACTIF).compteSysteme(true).build());

        System.out.println("""

            ╔════════════════════════════════════════════════════════════════╗
            ║ admin@entreprise.mg           Admin123!   ADMIN                 ║
            ║ sophie.martin@entreprise.mg   Chef123!    CHEF (ministère 05)   ║
            ║ hanta.rabe@entreprise.mg      Rh123!      RH (05 + L01)         ║
            ║ jean.rakoto@entreprise.mg     Agent123!   AGENT (123456)        ║
            ╚════════════════════════════════════════════════════════════════╝
            """);
    }

    // [SUPPRIMÉ] private Permission permission(String nom, String desc)

    // [MODIFIÉ] plus de paramètre Permission...
    private Role role(String nom, String desc) {
        return roleRepository.findByNomRole(nom)
                .orElseGet(() -> roleRepository.save(Role.builder().nomRole(nom).description(desc).build()));
    }

    // [MODIFIÉ] reçoit le rôle unique du compte
    private Utilisateur compte(String email, String motDePasse, String prenom, String nom, String matricule, Role role) {
        return utilisateurRepository.save(Utilisateur.builder()
                .email(email).motDePasseHash(passwordEncoder.encode(motDePasse))
                .prenom(prenom).nom(nom).matriculeAgent(matricule)
                .role(role)
                .statut(StatutUtilisateur.ACTIF).compteSysteme(false).build());
    }

    // [SUPPRIMÉ] private void assignRole(Utilisateur u, Role r)

    private void perimetre(Utilisateur u, DimensionPerimetre dimension, String code) {
        perimetreRepository.save(PerimetreUtilisateur.builder().utilisateur(u).dimension(dimension).code(code).build());
    }
}
