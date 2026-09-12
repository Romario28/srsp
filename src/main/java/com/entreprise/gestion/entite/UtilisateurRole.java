package com.entreprise.gestion.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Table de jonction UTILISATEUR_ROLE avec attribut propre (date_attribution).
 *
 * Stratégie : entité explicite + @IdClass(UtilisateurRoleId)
 * car la présence de date_attribution interdit l'usage d'un simple @ManyToMany.
 *
 * Clé primaire composite : (utilisateur.id, role.id)
 */
@Entity

@Table(name = "utilisateur_role", schema = "gestion_utilisateurs")

@IdClass(UtilisateurRoleId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UtilisateurRole {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_utilisateur",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_utilisateur_role_utilisateur")
    )

    private Utilisateur utilisateur;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_role",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_utilisateur_role_role")
    )
    private Role role;

    @Column(name = "date_attribution", nullable = false, updatable = false)
    private LocalDateTime dateAttribution;

    @PrePersist
    protected void onCreate() {
        this.dateAttribution = LocalDateTime.now();
    }
}
