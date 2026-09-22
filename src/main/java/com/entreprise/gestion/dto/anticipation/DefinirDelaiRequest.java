package com.entreprise.gestion.dto.anticipation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DefinirDelaiRequest {
    @NotNull @Min(0) private Integer prevenanceJours;   // MODIFIÉ — renommé depuis delaiPrevenanceJours
    @NotNull @Min(0) private Integer retardJours;        // AJOUTÉ
}
