package com.entreprise.gestion.entite.anticipation;

public enum TypeAnticipation {
    DEPART_RETRAITE,
    AVANCEMENT,          // désormais : changement d'échelon seulement (les deux premiers caractères du grade ne changent pas)
    AVANCEMENT_CLASSE,   // les deux premiers caractères du grade changent (ex. 2C2E → 1C3E)
    TITULARISATION,
    FIN_CONTRAT,
    INTEGRATION,
    ANOMALIE;            // donnée manquante empêchant un calcul — remonté, pas juste ignoré

    /** Types dont l'échéance est un changement de grade : le grade suivant est calculé et affiché. */
    public boolean changeDeGrade() {
        return this == AVANCEMENT || this == AVANCEMENT_CLASSE || this == TITULARISATION;
    }
}
