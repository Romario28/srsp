package com.entreprise.gestion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// [MODIFIÉ] role (obligatoire, un seul) remplace @NotEmpty List<String> roles
public record UpdateUtilisateurRequest(@Size(max = 100) String nom, @Size(max = 100) String prenom,
                                       @Size(max = 20) String matriculeAgent,
                                       @NotBlank String role, PerimetreDTO perimetre,
                                       boolean agirSurSonDossier) {}
