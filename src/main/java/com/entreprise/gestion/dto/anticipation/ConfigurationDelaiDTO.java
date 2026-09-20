// dto/anticipation/ConfigurationDelaiDTO.java
package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Fenêtre d'anticipation d'un type, telle qu'elle sera réellement appliquée
 * (les deux bornes effectives) comparée aux valeurs par défaut codées en dur.
 */
@Data
@AllArgsConstructor
public class ConfigurationDelaiDTO {
    private TypeAnticipation type;

    /** Borne « prévenance » EFFECTIVE (surchargée ou par défaut) — nb de jours avant l'échéance. */
    private int delaiPrevenanceJours;
    /** Prévenance par défaut codée en dur, pour comparaison. */
    private int delaiParDefaut;

    /** Borne « retard » EFFECTIVE — nb de jours après l'échéance ; 0 = retards masqués. */
    private int retardJours;
    /** Retard par défaut codé en dur, pour comparaison. */
    private int retardParDefaut;

    /** {@code true} si une surcharge est présente et active en base. */
    private boolean personnalise;
}
