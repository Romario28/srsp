package com.entreprise.gestion.dto;

import com.entreprise.gestion.entite.TypeAcces;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PorteeDelegueeDTO {
    private Long id;
    private Long idUtilisateur;
    private String emailUtilisateur;
    private Long idDepartement;
    private String nomDepartement;
    private TypeAcces typeAcces;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String accordePar;
    private boolean active; // calculé via PorteeDeleguee.estActive(today)
}