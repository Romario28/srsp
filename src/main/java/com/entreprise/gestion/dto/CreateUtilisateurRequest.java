package com.entreprise.gestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateUtilisateurRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @Size(max = 100) private String nom;
    @Size(max = 100) private String prenom;

    /** Lien informatif vers un agent (facultatif). N'ouvre aucun droit. */
    @Size(max = 20) private String matriculeAgent;

    private List<String> roles;          // vide : ROLE_GESTIONNAIRE
    private PerimetreDTO perimetre;      // [NOUVEAU] seule source des droits

    // [SUPPRIMÉ] private Long idEmploye;
}
