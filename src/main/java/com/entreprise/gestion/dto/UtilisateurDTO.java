package com.entreprise.gestion.dto;

import java.time.LocalDateTime;

// [MODIFIÉ] role (un seul) remplace roles (liste)
public record UtilisateurDTO(Long id, String email, String statut, LocalDateTime dateCreation,
                             LocalDateTime dateDerniereConnexion, String nomComplet, String matriculeAgent,
                             String role, PerimetreDTO perimetre,
                             boolean agirSurSonDossier) {}
