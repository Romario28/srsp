package com.entreprise.gestion.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Une valeur de périmètre accordée à un compte : (dimension, code).
 * Seule source des droits. Le code est un texte sans clé étrangère : un réimport des référentiels ne casse aucun compte.
 */
@Entity
@Table(name = "perimetre_utilisateur", schema = "gestion_utilisateurs",
        uniqueConstraints = @UniqueConstraint(name = "UQ_perimetre_utilisateur", columnNames = {"id_utilisateur", "dimension", "code"}),
        indexes = @Index(name = "idx_perimetre_utilisateur", columnList = "id_utilisateur"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PerimetreUtilisateur {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id_perimetre")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false, foreignKey = @ForeignKey(name = "FK_perimetre_utilisateur"))
    private Utilisateur utilisateur;

    @NotNull @Enumerated(EnumType.STRING) @Column(name = "dimension", nullable = false, length = 20)
    private DimensionPerimetre dimension;

    @NotNull @Size(max = 20) @Column(name = "code", nullable = false, length = 20)
    private String code;
}
