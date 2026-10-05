package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConfigurationDelaiDTO {
    private TypeAnticipation type;
    private int prevenanceMois;
    private int retardMois;
    private int prevenanceMoisDefaut;
    private int retardMoisDefaut;
    private boolean personnalise;
}
