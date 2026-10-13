package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.service.anticipation.EcheanceAnticipeeDTO;

/** Un agent concerne ce jour-la : sa categorie d'affichage et la ligne complete des ecrans d'echeances. */
public record CalendrierAgentDTO(CategorieCalendrier categorie, EcheanceAnticipeeDTO echeance) {}
