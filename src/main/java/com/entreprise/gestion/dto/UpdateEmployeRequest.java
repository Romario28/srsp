package com.entreprise.gestion.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class UpdateEmployeRequest {

    // matricule exclu volontairement : non modifiable

    @Size(max = 100)
    private String nom;

    @Size(max = 100)
    private String prenom;

    @Size(max = 100)
    private String poste;

    private Long idDepartement;

    private LocalDate dateEmbauche;
}