package com.entreprise.gestion.dto;

import java.time.LocalDateTime;
import java.util.List;

/** Fiche d'un compte : informations du compte, périmètre, et agent lié (null si aucun matricule). */
public record UtilisateurDetailDTO(Long id, String email, String statut, LocalDateTime dateCreation,
                                   LocalDateTime dateDerniereConnexion, String nom, String prenom,
                                   String matriculeAgent, List<String> roles, PerimetreDTO perimetre,
                                   AgentLieDTO agentLie,
                                   boolean agirSurSonDossier) {}
