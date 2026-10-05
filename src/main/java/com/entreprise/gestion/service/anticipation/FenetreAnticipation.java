package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.exception.BusinessException;

import java.time.LocalDate;

/**
 * prevenanceMois est le délai de préparation avant l'échéance ; retardMois est la tolérance après.
 * La fenêtre de visibilité est [date de préparation ; fin de tolérance].
 */
public record FenetreAnticipation(int prevenanceMois, int retardMois) {

    /** Plafond de 3 ans afin de borner les calculs calendaires. */
    public static final int MAX_MOIS = 36;

    public FenetreAnticipation {
        verifier(prevenanceMois, retardMois);
    }

    public LocalDate datePreparation(LocalDate echeance) {
        return echeance == null ? null : echeance.minusMonths(prevenanceMois);
    }

    public LocalDate dateFinTolerance(LocalDate echeance) {
        return echeance == null ? null : echeance.plusMonths(retardMois);
    }

    public boolean contient(LocalDate echeance, LocalDate aujourdhui) {
        if (echeance == null) return false;
        return !aujourdhui.isBefore(datePreparation(echeance))
                && !aujourdhui.isAfter(dateFinTolerance(echeance));
    }

    public static void verifier(int prevenanceMois, int retardMois) {
        if (prevenanceMois < 0 || retardMois < 0
                || prevenanceMois > MAX_MOIS || retardMois > MAX_MOIS) {
            throw new BusinessException("DELAI_INVALIDE",
                    "Les délais doivent être compris entre 0 et " + MAX_MOIS + " mois.");
        }
    }
}
