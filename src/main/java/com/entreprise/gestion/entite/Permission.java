package com.entreprise.gestion.entite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Permission atomique (ex: EMPLOYE_READ, EMPLOYE_WRITE, USER_MANAGE).
 * Côté inverse de la relation ManyToMany Role ↔ Permission.
 * La table de jonction ROLE_PERMISSION est gérée par @ManyToMany dans Role.
 */
@Entity
@Table(name = "permission", schema = "gestion_utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permission")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "nom_permission", nullable = false, unique = true, length = 100)
    private String nomPermission;

    @Column(name = "description", length = 255)
    private String description;

    /**
     * Côté inverse : mappedBy pointe vers le champ "permissions" dans Role.
     * JsonIgnore pour éviter la boucle Permission → Role → Permission.
     */
    @JsonIgnore
    @ManyToMany(mappedBy = "permissions", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
