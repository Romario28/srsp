package com.entreprise.gestion.service.anticipation;

// NOUVEAU FICHIER — encapsule les deux bornes (prévenance / retard) et la règle
// de visibilité, réutilisée à l'identique par le batch nocturne et les endpoints API.
/**
 * Fenêtre de visibilité d'une anticipation, en jours autour de l'échéance.
 *  - prevenanceJours (borne haute) : visible dès que joursRestants <= prevenanceJours.
 *  - retardJours (borne basse)     : visible tant que joursRestants >= -retardJours.
 *                                     0 = aucun retard toléré.
 * joursRestants garde son signe usuel : positif = à venir, négatif = dépassé.
 */
public record FenetreAnticipation(int prevenanceJours, int retardJours) {

    public boolean contient(long joursRestants) {
        return joursRestants <= prevenanceJours && joursRestants >= -retardJours;
    }
}
