package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sanction", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Sanction {
    @Id
    @Column(name = "sanction_code", length = 5)
    private String code;

    @Column(name = "sanction_libelle", length = 100)
    private String libelle;
}