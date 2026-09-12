package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "grade", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Grade {
    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Column(name = "libelle", length = 100)
    private String libelle;
}