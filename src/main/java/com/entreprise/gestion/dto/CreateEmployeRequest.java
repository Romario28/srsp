package com.entreprise.gestion.dto;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreateEmployeRequest {

    @NotBlank(message = "Le matricule est obligatoire")
    @Size(max = 20, message = "Matricule max 20 caractères")
    private String matricule;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(max = 100)
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(max = 100)
    private String prenom;

    @NotBlank(message = "Le poste est obligatoire")
    @Size(max = 100)
    private String poste;

    @NotNull(message = "Le département est obligatoire")
    private Long idDepartement;

    private LocalDate dateEmbauche; // optionnel
}