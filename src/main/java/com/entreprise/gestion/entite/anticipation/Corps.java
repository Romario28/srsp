package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "corps", schema = "referentiel_rh")
@IdClass(CorpsId.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Corps {
    @Id
    @Column(name = "code", length = 10)
    private String code;

    @Id
    @Column(name = "categorie", length = 10)
    private String categorie;

    @Column(name = "libelle", length = 150)
    private String libelle;
}
