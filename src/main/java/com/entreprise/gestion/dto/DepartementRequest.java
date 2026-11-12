package com.entreprise.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartementRequest(
        @NotBlank(message = "Le libellé est obligatoire") @Size(max = 200) String libelle) {}
