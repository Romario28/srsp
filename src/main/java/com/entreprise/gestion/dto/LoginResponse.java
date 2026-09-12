package com.entreprise.gestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String email;
    private String nomComplet;      // Nom + Prénom de l'employé lié, sinon email
    private List<String> roles;     // ["ROLE_ADMIN", ...]
    private String statut;
}
