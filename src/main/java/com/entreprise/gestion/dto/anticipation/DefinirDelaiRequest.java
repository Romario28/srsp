package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.service.anticipation.FenetreAnticipation;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DefinirDelaiRequest {
    @NotNull @Min(0) @Max(FenetreAnticipation.MAX_MOIS) private Integer prevenanceMois;
    @NotNull @Min(0) @Max(FenetreAnticipation.MAX_MOIS) private Integer retardMois;
}
