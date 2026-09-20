// entite/anticipation/ConfigurationDelai.java
package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

/**
 * Surcharge en base de la fenêtre d'anticipation d'un type : couple (prévenance, retard).
 * Une ligne absente — ou présente avec {@code actif = false} — signifie « utiliser
 * le couple par défaut codé en dur » (voir FenetresParDefaut).
 */
@Entity
@Table(name = "configuration_delai", schema = "anticipation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfigurationDelai {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type_anticipation", length = 30)
    private TypeAnticipation type;

    /** Borne « plus tôt » : jours avant l'échéance à partir desquels on prévient. */
    @Column(name = "delai_prevenance_jours", nullable = false)
    private Integer delaiPrevenanceJours;

    /**
     * Borne « plus tard » : jours de retard tolérés avant de ne plus afficher l'échéance.
     * 0 = les échéances dépassées ne sont jamais affichées.
     *
     * Nullable uniquement pour rester compatible avec les lignes créées avant l'ajout de
     * la colonne (ddl-auto=update en prod) : une valeur nulle est alors interprétée comme
     * « conserver le retard par défaut du type » par ConfigurationDelaiService.
     */
    @Column(name = "retard_jours")
    private Integer retardJours;

    /** Une surcharge inactive est ignorée par la résolution : on retombe sur le défaut. */
    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}
