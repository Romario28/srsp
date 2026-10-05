package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.exception.BusinessException;

import java.time.LocalDate;

/** Fenêtre de visibilité d'une anticipation en mois calendaires autour de l'échéance. */
public record FenetreAnticipation(int prevenanceMois, int retardMois) {

    /** Plafond de 3 ans afin de borner les calculs calendaires. */
    public static final int MAX_MOIS = 36;

    public FenetreAnticipation {
        verifier(prevenanceMois, retardMois);
    }

    public LocalDate borneHaute(LocalDate aujourdhui) {
        return aujourdhui.plusMonths(prevenanceMois);
    }

    public LocalDate borneBasse(LocalDate aujourdhui) {
        return aujourdhui.minusMonths(retardMois);
    }

    public boolean contient(LocalDate echeance, LocalDate aujourdhui) {
        if (echeance == null) return false;
        return !echeance.isAfter(borneHaute(aujourdhui))
                && !echeance.isBefore(borneBasse(aujourdhui));
    }

    public static void verifier(int prevenanceMois, int retardMois) {
        if (prevenanceMois < 0 || retardMois < 0
                || prevenanceMois > MAX_MOIS || retardMois > MAX_MOIS) {
            throw new BusinessException("DELAI_INVALIDE",
                    "Les délais doivent être compris entre 0 et " + MAX_MOIS + " mois.");
        }
    }
}
