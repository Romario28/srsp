package com.entreprise.gestion.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpdateUtilisateurRequest(@Size(max = 100) String nom, @Size(max = 100) String prenom,
                                       @Size(max = 20) String matriculeAgent,
                                       @NotEmpty List<String> roles, PerimetreDTO perimetre,
                                       boolean agirSurSonDossier) {}
