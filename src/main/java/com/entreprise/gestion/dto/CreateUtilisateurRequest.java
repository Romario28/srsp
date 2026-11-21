package com.entreprise.gestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUtilisateurRequest {

    @Email @NotBlank
    private String email;

    @NotBlank
    @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
    private String password;

    @Size(max = 100) private String nom;
    @Size(max = 100) private String prenom;

    /** Lien informatif vers un agent (facultatif, obligatoire pour un compte Agent). N'ouvre aucun droit. */
    @Size(max = 20) private String matriculeAgent;

    private String role;                 // [MODIFIÉ] un seul rôle (était : List<String> roles) ; vide : ROLE_AGENT
    private PerimetreDTO perimetre;
    private boolean agirSurSonDossier;   // profil Agent seulement ; ignoré sinon
}
