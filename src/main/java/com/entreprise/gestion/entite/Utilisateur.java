package com.entreprise.gestion.entite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compte système d'un utilisateur (employé ou administrateur).
 *
 * Relations :
 * ┌─────────────────────────────────────────────────────────────────┐
 * │  Groupe          ManyToOne   ← id_groupe (FK, nullable)         │
 * │  Employe         OneToOne    ← id_employe (FK, UNIQUE)          │
 * │  UtilisateurRole OneToMany   → table utilisateur_role           │
 * │  Session         OneToMany   → table session                    │
 * │  LogAudit        OneToMany   → table log_audit                  │
 * └─────────────────────────────────────────────────────────────────┘
 *
 * Note : les rôles sont accessibles via utilisateurRoles
 * (méthode helper getRoles() fournie pour simplifier l'usage dans Security).
 */
@Entity
@Table(
        name = "utilisateur",
        schema = "gestion_utilisateurs",
        indexes = {
                @Index(name = "idx_utilisateur_email", columnList = "email"),
                @Index(name = "idx_utilisateur_statut", columnList = "statut")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_utilisateur")
    private Long id;

    @NotNull
    @Email
    @Size(max = 150)
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @JsonIgnore
    @NotNull
    @Size(max = 255)
    @Column(name = "mot_de_passe_hash", nullable = false, length = 255)
    private String motDePasseHash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutUtilisateur statut = StatutUtilisateur.ACTIF;

    // entite/Utilisateur.java

    /**
     * Marque les comptes système protégés (ex : super-admin de secours).
     * Ces comptes sont :
     *  - exclus des listes retournées par l'API (findAll, findById)
     *  - protégés contre toute modification via l'API (changeStatut, futurs update/delete)
     *  - toujours capables de se connecter (login non filtré par ce flag)
     *
     * Impossible à définir via CreateUtilisateurRequest : le champ n'existe pas
     * dans le DTO, donc aucun endpoint REST ne peut jamais le positionner à true.
     * Seul DataInitializer (ou un accès direct à la base) peut créer un tel compte.
     */
    @Column(name = "compte_systeme", nullable = false)
    @Builder.Default
    private boolean compteSysteme = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_derniere_connexion")
    private LocalDateTime dateDerniereConnexion;


    // ── FK id_employe UNIQUE : un compte = au plus un employé ───────────────
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_employe",
            unique = true ,
            foreignKey = @ForeignKey(name = "FK_utilisateur_employe"))
    private Employe employe;

    // ── Rôles via table de jonction avec date_attribution ───────────────────
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UtilisateurRole> utilisateurRoles = new HashSet<>();

    // ── Sessions JWT actives ──────────────────────────────────────────────────
    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();

    // ── Logs d'audit ──────────────────────────────────────────────────────────
    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur")
    @Builder.Default
    private Set<LogAudit> logs = new HashSet<>();


    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    /**
     * Helper : retourne directement les entités Role sans passer par UtilisateurRole.
     * Utilisé par UserDetailsServiceImpl pour construire les GrantedAuthority.
     */
    public Set<Role> getRoles() {
        return utilisateurRoles.stream()
            .map(UtilisateurRole::getRole)
            .collect(Collectors.toSet());
    }
}
