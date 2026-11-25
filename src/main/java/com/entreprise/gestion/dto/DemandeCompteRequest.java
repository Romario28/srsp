package com.entreprise.gestion.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Demande publique de compte. Le profil demandé est vérifié par le service : jamais ROLE_ADMIN.
 * [MODIFIÉ] nom et prénom sont chacun facultatifs ; « au moins l'un des deux » est vérifié par le service (IDENTITE_REQUISE).
 */
public record DemandeCompteRequest(
        @NotBlank(message = "Le matricule est obligatoire") @Size(max = 20) String matricule,
        // SUPPRIMÉ : @NotBlank(message = "Le nom est obligatoire") @Size(max = 100) String nom,
        @Size(max = 100) String nom,      // [MODIFIÉ]
        // SUPPRIMÉ : @NotBlank(message = "Le prénom est obligatoire") @Size(max = 100) String prenom,
        @Size(max = 100) String prenom,   // [MODIFIÉ]
        @NotBlank(message = "L'e-mail est obligatoire") @Email @Size(max = 150) String email,
        @NotBlank(message = "Le mot de passe est obligatoire")
        @Size(min = 8, max = 72, message = "Le mot de passe doit contenir entre 8 et 72 caractères") String password,
        @NotBlank(message = "Le profil est obligatoire") String profil,
        @NotNull(message = "Le département est obligatoire") Long departementId) {

    /** Le mot de passe ne doit jamais apparaître dans les journaux (le niveau DEBUG de Spring MVC affiche le corps). */
    @Override public String toString() {
        return "DemandeCompteRequest[email=" + email + ", matricule=" + matricule + ", profil=" + profil
                + ", departementId=" + departementId + "]";
    }
}
