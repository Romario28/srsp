package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "indice_grade_corps", schema = "referentiel_rh")
@IdClass(IndiceGrdCorpsId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class IndiceGrdCorps {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_code")
    private Grade grade;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "corps_code", referencedColumnName = "code"),
            @JoinColumn(name = "categorie_code", referencedColumnName = "categorie")
    })
    private Corps corps;

    @Column(name = "indice", length = 20)
    private String indice;

    @Column(name = "duree_requise")
    private Integer dureeRequise;
}