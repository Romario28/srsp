// entite/anticipation/ConfigurationDelai.java
package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuration_delai", schema = "anticipation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfigurationDelai {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type_anticipation", length = 30)
    private TypeAnticipation type;

    @Column(name = "delai_prevenance_jours", nullable = false)
    private Integer delaiPrevenanceJours;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}