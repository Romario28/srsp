package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.util.Map;

/**
 * Fenêtres par défaut codées en dur — utilisées tant qu'aucune ConfigurationDelai
 * active n'existe en base. Les valeurs sont exprimées en mois calendaires.
 */
// MODIFIÉ — javadoc : un seul délai devient une fenêtre à deux bornes
public final class DelaisParDefaut {

    // MODIFIÉ — Map<TypeAnticipation, Integer> → Map<TypeAnticipation, FenetreAnticipation>
    // avant : TypeAnticipation.DEPART_RETRAITE, 548
    // après : un couple (prévenance, retard) par type
    private static final Map<TypeAnticipation, FenetreAnticipation> VALEURS = Map.of(
            TypeAnticipation.DEPART_RETRAITE, new FenetreAnticipation(12, 6),
            TypeAnticipation.AVANCEMENT,      new FenetreAnticipation(3, 1),
            TypeAnticipation.TITULARISATION,  new FenetreAnticipation(3, 1),
            TypeAnticipation.FIN_CONTRAT,     new FenetreAnticipation(3, 1)
            // ANOMALIE : pas de fenêtre — toujours remontée, voir AnticipationService.anomalies()
    );

    // MODIFIÉ — "static int pour(...)" devient "static FenetreAnticipation pour(...)"
    public static FenetreAnticipation pour(TypeAnticipation type) {
        FenetreAnticipation v = VALEURS.get(type);
        if (v == null) {
            throw new IllegalArgumentException("Aucune fenêtre par défaut définie pour : " + type);
        }
        return v;
    }

    private DelaisParDefaut() {}
}
