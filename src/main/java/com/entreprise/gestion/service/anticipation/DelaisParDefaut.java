package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.util.Map;

/** Valeurs par défaut codées en dur — utilisées tant qu'aucune ConfigurationDelai active n'existe pour le type. */
public final class DelaisParDefaut {

    private static final Map<TypeAnticipation, Integer> VALEURS = Map.of(
            TypeAnticipation.DEPART_RETRAITE, 548,   // 18 mois
            TypeAnticipation.AVANCEMENT,      90,
            TypeAnticipation.TITULARISATION,  90,
            TypeAnticipation.FIN_CONTRAT,     90
            // ANOMALIE : pas de délai — toujours remontée, voir AnticipationService.anomalies()
    );

    public static int pour(TypeAnticipation type) {
        Integer v = VALEURS.get(type);
        if (v == null) {
            throw new IllegalArgumentException("Aucun délai par défaut défini pour : " + type);
        }
        return v;
    }

    private DelaisParDefaut() {}
}