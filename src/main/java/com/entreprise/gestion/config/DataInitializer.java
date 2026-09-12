package com.entreprise.gestion.config;

import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.*;
import com.entreprise.gestion.repository.anticipation.ConfigurationDelaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Scénario de démonstration :
 *
 *   admin@entreprise.mg          ADMIN            → voit TOUT
 *   tiana / sophie / miora / jean / hery  chef123! (chef de département)
 *                                                     → voit son propre sous-arbre
 *   jean.dupont@...              + délégation temporaire LECTURE sur DRH
 *                                                     accordée par Voahangy (pas par ADMIN)
 *   fidy.rakoto@...               simple employé    → lui-même uniquement
 *   voahangy.rasoamanana@...      délégation LECTURE_ECRITURE PERMANENTE sur SG (racine)
 *                                                     → équivaut à "RH central" sans être ADMIN
 *   hanta.rabe@...                délégation LECTURE_ECRITURE PERMANENTE sur DFI,
 *                                                     accordée par Sophie (chef DFI, pas ADMIN)
 *                                                     → équivaut à "RH local" pour la DFI
 */
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository            roleRepository;
    private final PermissionRepository      permissionRepository;
    private final DepartementRepository     departementRepository;
    private final EmployeRepository         employeRepository;
    private final UtilisateurRepository     utilisateurRepository;
    private final UtilisateurRoleRepository utilisateurRoleRepository;
    private final PorteeDelegueeRepository  porteeDelegueeRepository;
    private final PasswordEncoder           passwordEncoder;
    private final ConfigurationDelaiRepository configurationDelaiRepository;

    @Override
    @Transactional
    public void run(String... args) {

        Permission pRead   = permissionRepository.save(perm("EMPLOYE_READ",  "Consulter les employés"));
        Permission pWrite  = permissionRepository.save(perm("EMPLOYE_WRITE", "Créer/modifier un employé"));
        Permission pDelete = permissionRepository.save(perm("EMPLOYE_DELETE","Supprimer un employé"));
        Permission pUser   = permissionRepository.save(perm("USER_MANAGE",   "Gérer les comptes"));
        Permission pAudit  = permissionRepository.save(perm("AUDIT_READ",    "Consulter l'audit"));
        Permission pOrg    = permissionRepository.save(perm("ORGANIGRAMME_MANAGE", "Gérer l'organigramme"));

        Role roleEmploye = save(role("ROLE_EMPLOYE", "Authentifié — périmètre dérivé (chef/délégations)", pRead));
        Role roleAdmin   = save(role("ROLE_ADMIN", "Accès complet", pRead, pWrite, pDelete, pUser, pAudit, pOrg));

        Departement sg  = creerDepartement("Secrétariat Général", "Secrétariat Général", null);
        Departement dgb = creerDepartement("Direction Générale du Budget", "Direction Générale", sg);
        Departement dfi = creerDepartement("Direction des Finances", "Direction", dgb);
        Departement drh = creerDepartement("Direction des Ressources Humaines", "Direction", dgb);
        Departement svcCompta = creerDepartement("Service Comptabilité", "Service", dfi);
        Departement divTresor = creerDepartement("Division Trésorerie", "Division", svcCompta);

        Employe andry    = employeRepository.save(emp("EMP001", "Rakoto", "Andry", "Secrétaire Général", sg, LocalDate.of(2015,1,10)));
        Employe tiana    = employeRepository.save(emp("EMP002", "Rasoanaivo", "Tiana", "Directrice Générale du Budget", dgb, LocalDate.of(2017,4,3)));
        Employe sophie   = employeRepository.save(emp("EMP003", "Martin", "Sophie", "Directrice des Finances", dfi, LocalDate.of(2018,6,12)));
        Employe miora    = employeRepository.save(emp("EMP004", "Randria", "Miora", "Directrice des Ressources Humaines", drh, LocalDate.of(2019,2,20)));
        Employe jean     = employeRepository.save(emp("EMP005", "Dupont", "Jean", "Chef du Service Comptabilité", svcCompta, LocalDate.of(2020,6,1)));
        Employe fidy     = employeRepository.save(emp("EMP006", "Rakoto", "Fidy", "Agent comptable", svcCompta, LocalDate.of(2022,9,12)));
        Employe hery     = employeRepository.save(emp("EMP007", "Andria", "Hery", "Chef de la Division Trésorerie", divTresor, LocalDate.of(2019,1,10)));
        Employe nina     = employeRepository.save(emp("EMP008", "Razafy", "Nina", "Agent de trésorerie", divTresor, LocalDate.of(2023,3,5)));
        Employe voahangy = employeRepository.save(emp("EMP009", "Rasoamanana", "Voahangy", "Chargée d'audit RH", sg, LocalDate.of(2016,5,15)));
        Employe hanta    = employeRepository.save(emp("EMP010", "Rabe", "Hanta", "Chargée RH — Finances", dfi, LocalDate.of(2021,8,1)));

        sg.setChef(andry);        departementRepository.save(sg);
        dgb.setChef(tiana);       departementRepository.save(dgb);
        dfi.setChef(sophie);      departementRepository.save(dfi);
        drh.setChef(miora);       departementRepository.save(drh);
        svcCompta.setChef(jean);  departementRepository.save(svcCompta);
        divTresor.setChef(hery);  departementRepository.save(divTresor);

        Utilisateur admin     = save(utilisateur("admin@entreprise.mg", "Admin123!", null));
        assignRole(admin, roleAdmin);

        Utilisateur uTiana    = save(utilisateur("tiana.rasoanaivo@entreprise.mg", "Chef123!", tiana));
        assignRole(uTiana, roleEmploye);

        Utilisateur uSophie   = save(utilisateur("sophie.martin@entreprise.mg", "Chef123!", sophie));
        assignRole(uSophie, roleEmploye);

        Utilisateur uMiora    = save(utilisateur("miora.randria@entreprise.mg", "Chef123!", miora));
        assignRole(uMiora, roleEmploye);

        Utilisateur uJean     = save(utilisateur("jean.dupont@entreprise.mg", "Chef123!", jean));
        assignRole(uJean, roleEmploye);

        Utilisateur uFidy     = save(utilisateur("fidy.rakoto@entreprise.mg", "Employe123!", fidy));
        assignRole(uFidy, roleEmploye);

        Utilisateur uHery     = save(utilisateur("hery.andria@entreprise.mg", "Chef123!", hery));
        assignRole(uHery, roleEmploye);

        Utilisateur uNina     = save(utilisateur("nina.razafy@entreprise.mg", "Employe123!", nina));
        assignRole(uNina, roleEmploye);

        Utilisateur uVoahangy = save(utilisateur("voahangy.rasoamanana@entreprise.mg", "Delegue123!", voahangy));
        assignRole(uVoahangy, roleEmploye);

        Utilisateur uHanta    = save(utilisateur("hanta.rabe@entreprise.mg", "Delegue123!", hanta));
        assignRole(uHanta, roleEmploye);

        String motDePasseSecours = System.getenv().getOrDefault("SUPERADMIN_PASSWORD", "SuperSecret_ChangeMe_2024!");
        Utilisateur superAdmin = utilisateurRepository.save(Utilisateur.builder()
                .email("superadmin@entreprise.mg")
                .motDePasseHash(passwordEncoder.encode(motDePasseSecours))
                .statut(StatutUtilisateur.ACTIF).compteSysteme(true).build());
        assignRole(superAdmin, roleAdmin);

        porteeDelegueeRepository.save(PorteeDeleguee.builder()
                .utilisateur(uVoahangy).departement(sg).typeAcces(TypeAcces.LECTURE_ECRITURE)
                .dateDebut(LocalDate.now()).dateFin(null).accordePar(admin).build());

        porteeDelegueeRepository.save(PorteeDeleguee.builder()
                .utilisateur(uHanta).departement(dfi).typeAcces(TypeAcces.LECTURE_ECRITURE)
                .dateDebut(LocalDate.now()).dateFin(null).accordePar(uSophie).build());

        porteeDelegueeRepository.save(PorteeDeleguee.builder()
                .utilisateur(uJean).departement(drh).typeAcces(TypeAcces.LECTURE)
                .dateDebut(LocalDate.now()).dateFin(LocalDate.now().plusMonths(1)).accordePar(uVoahangy).build());

//        configurationDelaiRepository.saveAll(List.of(
//                ConfigurationDelai.builder().type(TypeAnticipation.DEPART_RETRAITE).delaiPrevenanceJours(365).build(),
//                ConfigurationDelai.builder().type(TypeAnticipation.AVANCEMENT).delaiPrevenanceJours(30).build(),
//                ConfigurationDelai.builder().type(TypeAnticipation.TITULARISATION).delaiPrevenanceJours(60).build(),
//                ConfigurationDelai.builder().type(TypeAnticipation.FIN_CONTRAT).delaiPrevenanceJours(90).build()
//        ));
        System.out.println("""

            ╔═══════════════════════════════════════════════════════════════╗
            ║        Données de démonstration MEF initialisées               ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║ admin@entreprise.mg              Admin123!    ADMIN            ║
            ║ voahangy.rasoamanana@...         Delegue123!  (délégation racine — RH central) ║
            ║ hanta.rabe@...                   Delegue123!  (délégation DFI — RH local)   ║
            ║ tiana / sophie / miora / jean / hery   Chef123!  (chef de département)      ║
            ║ jean.dupont@...                  + délégation temporaire LECTURE sur DRH    ║
            ║ fidy.rakoto@... / nina.razafy@... Employe123!  (voient uniquement eux-mêmes) ║
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

    private Role save(Role r) { return roleRepository.save(r); }
    private Utilisateur save(Utilisateur u) { return utilisateurRepository.save(u); }

    private Departement creerDepartement(String nom, String niveau, Departement parent) {
        Departement d = Departement.builder().nomDepartement(nom).niveau(niveau).departementParent(parent).build();
        Departement saved = departementRepository.save(d);
        saved.setChemin((parent != null ? parent.getChemin() : "/") + saved.getId() + "/");
        return departementRepository.save(saved);
    }

    private Employe emp(String matricule, String nom, String prenom, String poste,
                        Departement departement, LocalDate dateEmbauche) {
        return Employe.builder().matricule(matricule).nom(nom).prenom(prenom)
                .poste(poste).departement(departement).dateEmbauche(dateEmbauche).build();
    }

    private Utilisateur utilisateur(String email, String motDePasse, Employe employe) {
        return Utilisateur.builder()
                .email(email).motDePasseHash(passwordEncoder.encode(motDePasse))
                .statut(StatutUtilisateur.ACTIF).employe(employe).compteSysteme(false).build();
    }

    private void assignRole(Utilisateur utilisateur, Role role) {
        utilisateurRoleRepository.save(UtilisateurRole.builder().utilisateur(utilisateur).role(role).build());
    }
}