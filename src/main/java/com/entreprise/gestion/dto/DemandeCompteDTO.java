package com.entreprise.gestion.dto;

import java.time.LocalDateTime;

/** Une demande en attente, vue par un validateur. agentLie : l'agent de ce matricule (trouve = faux s'il est absent de la base). */
public record DemandeCompteDTO(Long id, String email, String nom, String prenom, String matricule, String profil,
                               LocalDateTime dateDemande, AgentLieDTO agentLie, PerimetreDTO perimetreSuggere) {}
