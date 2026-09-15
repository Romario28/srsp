// dto/anticipation/DefinirDelaiRequest.java
package com.entreprise.gestion.dto.anticipation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DefinirDelaiRequest {
    @NotNull
    @Min(0)
    private Integer delaiPrevenanceJours;
}