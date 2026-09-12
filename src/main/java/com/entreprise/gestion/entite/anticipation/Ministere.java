// Ministere.java
package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ministere", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Ministere {
    @Id
    @Column(name = "min_code", length = 5)
    private String code;

    @Column(name = "min_libelle", length = 150)
    private String libelle;
}