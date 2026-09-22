package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "configuration_delai", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfigurationDelai {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type_anticipation", length = 30)
    private TypeAnticipation type;

    @Column(name = "delai_prevenance_jours", nullable = false)
    private Integer delaiPrevenanceJours;

    // AJOUTÉ — borne de retard. Colonne NOT NULL sur table pouvant déjà contenir
    // des lignes : migration DB nécessaire en profil prod (ALTER TABLE ... ADD COLUMN ... DEFAULT).
    @Column(name = "delai_retard_jours", nullable = false)
    private Integer delaiRetardJours;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}
