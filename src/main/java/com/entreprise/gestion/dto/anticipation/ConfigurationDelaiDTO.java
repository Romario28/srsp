package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigurationDelaiDTO {
    private TypeAnticipation type;
    // MODIFIÉ — "delaiPrevenanceJours"/"delaiParDefaut" (1 borne) → 2 champs par borne
    private int prevenanceJours;        // valeur EFFECTIVE
    private int retardJours;            // AJOUTÉ
    private int prevenanceJoursDefaut;
    private int retardJoursDefaut;      // AJOUTÉ
    private boolean personnalise;
}
