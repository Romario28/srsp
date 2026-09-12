package com.entreprise.gestion.dto;

import com.entreprise.gestion.entite.Employe;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;

@Data
@Builder
public class EmployeResponse {

    private Long      id;
    private String    matricule;
    private String    nom;
    private String    prenom;
    private String    poste;
    private Long      idDepartement;    // pour préremplir un formulaire d'édition
    private String    nomDepartement;   // pour l'affichage
    private LocalDate dateEmbauche;
    private boolean   aUnCompte;
    private boolean   estChef;          // NOUVEAU : true si chef officiel de son département
    private String    nomSuperieur;     // NOUVEAU : dérivé via PorteeService.getSuperieur() — voir EmployeService.findById

    private Long   idManager;
    private String nomManager;

    public static EmployeResponse from(Employe e) {
        return EmployeResponse.builder()
                .id(e.getId())
                .matricule(e.getMatricule())
                .nom(e.getNom())
                .prenom(e.getPrenom())
                .poste(e.getPoste())
                .idDepartement(e.getDepartement() != null ? e.getDepartement().getId() : null)
                .nomDepartement(e.getDepartement() != null ? e.getDepartement().getNomDepartement() : null)
                .dateEmbauche(e.getDateEmbauche())
                .aUnCompte(false)   // renseigné par le service (voir EmployeService.findAll/findById)
                .estChef(e.getDepartement() != null
                        && e.getDepartement().getChef() != null
                        && e.getId().equals(e.getDepartement().getChef().getId()))
                .build();
    }

}
