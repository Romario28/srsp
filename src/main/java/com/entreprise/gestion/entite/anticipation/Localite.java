package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "localite", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Localite {
    @Id
    @Column(name = "code_localite", length = 10)
    private String code;

    @Column(name = "localite", length = 100)
    private String nom;
}