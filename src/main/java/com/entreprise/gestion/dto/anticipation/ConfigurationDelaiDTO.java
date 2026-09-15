// dto/anticipation/ConfigurationDelaiDTO.java
package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigurationDelaiDTO {
    private TypeAnticipation type;
    private int delaiPrevenanceJours; // valeur EFFECTIVE (surchargée ou par défaut)
    private int delaiParDefaut;       // valeur par défaut codée en dur, pour comparaison
    private boolean personnalise;     // true si une surcharge active existe en base
}