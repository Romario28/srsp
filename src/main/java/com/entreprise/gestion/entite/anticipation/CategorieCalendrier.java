package com.entreprise.gestion.entite.anticipation;

/** Catégorie d'affichage du calendrier : les avancements ELD sont séparés. */
public enum CategorieCalendrier {
    // AVANCEMENT_CLASSE ajouté après AVANCEMENT (l'ordre est celui de la légende et du tri)
    DEPART_RETRAITE, AVANCEMENT, AVANCEMENT_CLASSE, ELD, TITULARISATION, FIN_CONTRAT, INTEGRATION;

    /** Les anomalies n'ont pas de date et ne sont donc pas placées dans le calendrier. */
    public static CategorieCalendrier de(TypeAnticipation type, StatutAgent statut) {
        return switch (type) {
            case DEPART_RETRAITE -> DEPART_RETRAITE;
            case AVANCEMENT -> statut == StatutAgent.ELD ? ELD : AVANCEMENT;
            case AVANCEMENT_CLASSE -> AVANCEMENT_CLASSE;   // le type prime sur le statut, ELD compris
            case TITULARISATION -> TITULARISATION;
            case FIN_CONTRAT -> FIN_CONTRAT;
            case INTEGRATION -> INTEGRATION;
            case ANOMALIE -> null;
        };
    }
}
