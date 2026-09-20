package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.util.Map;

/**
 * Couples (prévenance, retard) codés en dur — utilisés tant qu'aucune configuration
 * ACTIVE n'existe en base pour le type (voir {@link ConfigurationDelaiService#resoudreFenetre}).
 *
 * Ces valeurs ne sont jamais écrites en base au démarrage : la base ne contient que des
 * surcharges explicites, ce qui rend la réinitialisation (retour au défaut) triviale.
 */
public final class FenetresParDefaut {

    private static final Map<TypeAnticipation, FenetreAnticipation> VALEURS = Map.of(
            TypeAnticipation.DEPART_RETRAITE, new FenetreAnticipation(548, 365),  // 18 mois avant / 12 mois de retard toléré
            TypeAnticipation.AVANCEMENT,      new FenetreAnticipation(90, 30),    // 3 mois avant / 1 mois de retard
            TypeAnticipation.TITULARISATION,  new FenetreAnticipation(90, 30),
            TypeAnticipation.FIN_CONTRAT,     new FenetreAnticipation(90, 30)
            // ANOMALIE : aucune fenêtre — toujours remontée, voir AnticipationService.anomalies()
    );

    /** Fenêtre par défaut du type — lève une exception si le type n'a pas de fenêtre (ANOMALIE). */
    public static FenetreAnticipation pour(TypeAnticipation type) {
        FenetreAnticipation v = VALEURS.get(type);
        if (v == null) {
            throw new IllegalArgumentException("Aucune fenêtre par défaut définie pour : " + type);
        }
        return v;
    }

    /** Les 4 types pilotables en base de données (ANOMALIE exclue). */
    public static boolean configurable(TypeAnticipation type) {
        return VALEURS.containsKey(type);
    }

    private FenetresParDefaut() {}
}
