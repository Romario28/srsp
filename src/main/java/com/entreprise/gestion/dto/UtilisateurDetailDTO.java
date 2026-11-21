package com.entreprise.gestion.dto;

import java.time.LocalDateTime;

/** Fiche d'un compte : informations du compte, périmètre, et agent lié (null si aucun matricule). */
// [MODIFIÉ] role (un seul) remplace roles (liste)
public record UtilisateurDetailDTO(Long id, String email, String statut, LocalDateTime dateCreation,
                                   LocalDateTime dateDerniereConnexion, String nom, String prenom,
                                   String matriculeAgent, String role, PerimetreDTO perimetre,
                                   AgentLieDTO agentLie,
                                   boolean agirSurSonDossier) {}
