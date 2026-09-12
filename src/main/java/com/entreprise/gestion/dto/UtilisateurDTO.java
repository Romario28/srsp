package com.entreprise.gestion.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UtilisateurDTO {
    private Long id;
    private String email;
    private String statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateDerniereConnexion;
    private String nomEmploye;      // Prénom + Nom de l'employé lié
    private String matriculeEmploye;
    private String nomDepartement;
    private List<String> roles;     // Noms des rôles
}
