package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;

import java.time.LocalDate;

/** Nombre d'échéances d'une catégorie ayant ces deux dates. */
public record CalendrierJourDTO(LocalDate datePreparation, LocalDate dateEcheance,
                                CategorieCalendrier categorie, long nombre) {}
