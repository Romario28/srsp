package com.entreprise.gestion.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Service ou direction où travaille un utilisateur. Information seulement : aucun lien avec le périmètre d'accès
 * (ministère, SOA, localité). Sert à situer la personne et à préparer ses documents.
 */
@Entity
@Table(name = "departement", schema = "gestion_utilisateurs",
        uniqueConstraints = @UniqueConstraint(name = "UQ_departement_libelle", columnNames = "libelle"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Departement {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departement")
    private Long id;

    @NotNull @Size(max = 200)
    @Column(name = "libelle", nullable = false, length = 200)
    private String libelle;
}
