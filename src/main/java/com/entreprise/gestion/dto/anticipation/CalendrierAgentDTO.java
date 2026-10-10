package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.entite.anticipation.StatutAgent;

import java.time.LocalDate;

/** Agent concerné par une échéance à une date donnée du calendrier. */
public record CalendrierAgentDTO(String matricule, String nomComplet, CategorieCalendrier categorie,
                                StatutAgent statut, LocalDate datePreparation, LocalDate dateEcheance) {}
