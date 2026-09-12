package com.entreprise.gestion.dto;

import com.entreprise.gestion.entite.TypeAcces;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDate;

@Data
public class CreatePorteeDelegueeRequest {
    @NotNull private Long idUtilisateur;
    @NotNull private Long idDepartement;
    @NotNull private TypeAcces typeAcces;
    @NotNull private LocalDate dateDebut; // peut être future (planification)
    private LocalDate dateFin;            // null = permanente
}