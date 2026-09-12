package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "hee", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Hee {
    @Id
    @Column(name = "hee_code", length = 10)
    private String code;

    @Column(name = "libelle", length = 150)
    private String libelle;
}