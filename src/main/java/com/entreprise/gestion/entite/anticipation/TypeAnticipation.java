package com.entreprise.gestion.entite.anticipation;

public enum TypeAnticipation {
    DEPART_RETRAITE,
    AVANCEMENT,
    TITULARISATION,
    FIN_CONTRAT,
    ANOMALIE   // donnée manquante empêchant un calcul — remonté, pas juste ignoré
}