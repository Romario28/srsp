package com.entreprise.gestion.entite.anticipation;

/** Raison pour laquelle une échéance ne peut pas être calculée. */
public enum TypeAnomalie {
    DATE_NAISSANCE_MANQUANTE("Date de naissance manquante"),
    GRADE_ET_CORPS_MANQUANTS("Grade et corps manquants"),
    GRADE_MANQUANT("Grade manquant"),
    CORPS_MANQUANT("Corps manquant"),
    ANCRAGE_MANQUANT("Date d'avancement et début de contrat manquants"),
    DUREE_REQUISE_MANQUANTE("Durée requise non renseignée");

    private final String libelle;
    TypeAnomalie(String libelle) { this.libelle = libelle; }
    public String libelle() { return libelle; }
}
