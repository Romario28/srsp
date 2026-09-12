package com.entreprise.gestion.entite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Rôle RBAC (ex: ROLE_ADMIN, ROLE_MANAGER, ROLE_EMPLOYE).
 *
 * Deux relations Many-to-Many de natures différentes :
 *
 * 1. Role ↔ Permission  →  table ROLE_PERMISSION sans colonne extra
 *    Géré directement par @ManyToMany + @JoinTable (pas d'entité Java dédiée).
 *
 * 2. Role ↔ Utilisateur →  table UTILISATEUR_ROLE avec date_attribution
 *    Géré par l'entité explicite UtilisateurRole (côté @OneToMany ici).
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

    // ── Role ↔ Permission (table ROLE_PERMISSION, pas de colonne extra) ──────
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "role_permission",
            schema = "gestion_utilisateurs",
            joinColumns = @JoinColumn(
                    name = "id_role",
                    foreignKey = @ForeignKey(name = "FK_role_permission_role")
            ),
            inverseJoinColumns = @JoinColumn(
                    name = "id_permission",
                    foreignKey = @ForeignKey(name = "FK_role_permission_permission")
            )
    )

    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();

    // ── Role ↔ UtilisateurRole (table UTILISATEUR_ROLE, avec date_attribution) ──
    @JsonIgnore
    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<UtilisateurRole> utilisateurRoles = new HashSet<>();
}
