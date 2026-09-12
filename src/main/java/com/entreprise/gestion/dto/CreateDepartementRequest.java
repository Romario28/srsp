package com.entreprise.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateDepartementRequest {
    @NotBlank private String nomDepartement;
    private String niveau; // libellé libre — "Direction", "Service"...
    private Long idDepartementParent; // null = racine
    private String description;
}