package com.entreprise.gestion.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Rôle (ROLE_ADMIN, ROLE_CHEF, ROLE_RH, ROLE_AGENT).
 * Relation avec Utilisateur : chaque utilisateur porte exactement un rôle (Utilisateur.role, obligatoire) ;
 * un rôle peut être attribué à zéro, un ou plusieurs utilisateurs.
 * [MODIFIÉ] plus de permissions (table role_permission supprimée) ni de table de jonction avec les utilisateurs.
 */
@Entity
@Table(name = "role", schema = "gestion_utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_role")
    private Long id;

    @NotNull
    @Size(max = 50)
    @Column(name = "nom_role", nullable = false, unique = true, length = 50)
    private String nomRole;

    @Column(name = "description", length = 255)
    private String description;

    // [SUPPRIMÉ] Set<Permission> permissions (@ManyToMany, table role_permission)
    // [SUPPRIMÉ] Set<UtilisateurRole> utilisateurRoles (@OneToMany vers la table de jonction)
}
