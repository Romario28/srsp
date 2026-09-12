package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "soa", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Soa {
    @Id
    @Column(name = "soa", length = 20)
    private String code;

    @Column(name = "soa_libelle", length = 150)
    private String libelle;
}