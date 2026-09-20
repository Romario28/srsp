// entite/anticipation/ConfigurationDelai.java
package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;

/**
 * Surcharge en base d'une fenêtre d'anticipation (voir
 * {@link com.entreprise.gestion.service.anticipation.FenetreAnticipation}).
 *
 * <p>Une ligne absente, ou présente mais avec {@code actif = false}, signifie
 * « utiliser la fenêtre par défaut codée en dur ». Les deux bornes peuvent donc
 * être modifiées directement en base (ou via l'API de configuration) sans
 * redéploiement.</p>
 */
@Entity
@Table(name = "configuration_delai", schema = "anticipation")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConfigurationDelai {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "type_anticipation", length = 30)
    private TypeAnticipation type;

    /**
     * Borne haute : nombre de jours AVANT l'échéance à partir duquel l'anticipation
     * devient visible (ex. 120 = 4 mois de préavis).
     */
    @Column(name = "delai_prevenance_jours")
    private Integer delaiPrevenanceJours;

    /**
     * Borne basse : nombre de jours de retard (échéance dépassée) au-delà duquel
     * l'anticipation n'est plus affichée. {@code 0} = ne jamais afficher les retards.
     *
     * <p>Colonne volontairement nullable : une ligne héritée, créée avant l'introduction
     * de cette borne, n'a pas de retard défini et retombe alors sur le retard par défaut
     * de son type (aucune migration bloquante n'est nécessaire).</p>
     */
    @Column(name = "retard_jours")
    private Integer retardJours;

    /** Une surcharge désactivée est ignorée : le type retombe sur sa valeur par défaut. */
    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;
}
