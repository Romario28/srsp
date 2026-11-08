package com.entreprise.gestion.dto;

import java.time.LocalDateTime;
import java.util.List;

public record UtilisateurDTO(Long id, String email, String statut, LocalDateTime dateCreation,
                             LocalDateTime dateDerniereConnexion, String nomComplet, String matriculeAgent,
                             List<String> roles, PerimetreDTO perimetre,
                             boolean agirSurSonDossier) {}
