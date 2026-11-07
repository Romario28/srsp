package com.entreprise.gestion.entite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Compte d'un utilisateur de l'application.
 *
 * matriculeAgent : lien INFORMATIF vers un agent (texte, nullable, sans clé étrangère). Il identifie la personne
 * physique (nom affiché, journal, conflit d'intérêts) et ne donne AUCUN droit : les droits viennent
 * exclusivement de PerimetreUtilisateur.
 */
@Entity
@Table(
        name = "utilisateur",
        schema = "gestion_utilisateurs",
        indexes = {
                @Index(name = "idx_utilisateur_email", columnList = "email"),
                @Index(name = "idx_utilisateur_statut", columnList = "statut")
        },
        uniqueConstraints = @UniqueConstraint(name = "UQ_utilisateur_matricule_agent", columnNames = "matricule_agent")   // [NOUVEAU] un agent, un seul compte
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

    // [NOUVEAU] ↓ identité de la personne, copiée de l'agent au moment du lien (modifiable)
    @Size(max = 100)
    @Column(name = "nom", length = 100)
    private String nom;

    @Size(max = 100)
    @Column(name = "prenom", length = 100)
    private String prenom;

    /** Lien informatif vers l'agent : texte, nullable, SANS clé étrangère. N'ouvre aucun droit. */
    @Size(max = 20)
    @Column(name = "matricule_agent", length = 20)
    private String matriculeAgent;
    // [NOUVEAU] ↑

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "statut", nullable = false, length = 20)
    @Builder.Default
    private StatutUtilisateur statut = StatutUtilisateur.ACTIF;

    /** Comptes système protégés (super-admin de secours) : exclus de l'API, non modifiables, toujours capables de se connecter. */
    @Column(name = "compte_systeme", nullable = false)
    @Builder.Default
    private boolean compteSysteme = false;

    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_derniere_connexion")
    private LocalDateTime dateDerniereConnexion;

    // [SUPPRIMÉ] @OneToOne Employe employe;

    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UtilisateurRole> utilisateurRoles = new HashSet<>();

    // [NOUVEAU] ↓ seule source des droits ; BatchSize évite une requête par compte dans la liste
    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 50)
    @Builder.Default
    private Set<PerimetreUtilisateur> perimetres = new HashSet<>();
    // [NOUVEAU] ↑

    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "utilisateur")
    @Builder.Default
    private Set<LogAudit> logs = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }

    public Set<Role> getRoles() {
        return utilisateurRoles.stream()
            .map(UtilisateurRole::getRole)
            .collect(Collectors.toSet());
    }

    // [NOUVEAU] ↓
    /** « Prénom Nom », chaîne vide si aucun des deux n'est renseigné. */
    public String nomComplet() {
        String p = prenom == null ? "" : prenom.trim();
        String n = nom == null ? "" : nom.trim();
        return (p + " " + n).trim();
    }

    /** Libellé de l'acteur pour le journal : nom complet et matricule si le compte est lié à un agent, sinon l'e-mail. */
    public String libelleActeur() {
        if (matriculeAgent == null) return email;
        String nomComplet = nomComplet();
        return (nomComplet.isEmpty() ? email : nomComplet) + " (matricule " + matriculeAgent + ")";
    }
    // [NOUVEAU] ↑
}
