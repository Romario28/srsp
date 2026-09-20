package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Couples prévenance/retard codés en dur, utilisés tant qu'aucune configuration
 * <em>active</em> n'existe en base pour le type d'anticipation considéré.
 *
 * <p>C'est l'unique endroit où modifier les valeurs par défaut : elles restent
 * surchargeables sans redéploiement, via la table {@code anticipation.configuration_delai}
 * (voir {@code docs/configuration_anticipation.md}).</p>
 *
 * <p>Type {@code ANOMALIE} volontairement absent : une anomalie de donnée n'a pas
 * d'échéance, elle est donc toujours remontée, quel que soit le délai configuré.</p>
 */
public final class FenetresParDefaut {

    private static final Map<TypeAnticipation, FenetreAnticipation> VALEURS;

    static {
        Map<TypeAnticipation, FenetreAnticipation> valeurs = new EnumMap<>(TypeAnticipation.class);

        // Retraite : préavis large (18 mois) pour préparer le dossier de pension ;
        // un départ déjà dépassé reste visible un an, le temps de le régulariser.
        valeurs.put(TypeAnticipation.DEPART_RETRAITE, new FenetreAnticipation(548, 365));

        // Avancement (classe / échelon) : 3 mois de préavis ; un avancement manqué
        // reste remonté un an (régularisation avec effet rétroactif).
        valeurs.put(TypeAnticipation.AVANCEMENT, new FenetreAnticipation(90, 365));

        // Titularisation d'un stagiaire : même logique que l'avancement.
        valeurs.put(TypeAnticipation.TITULARISATION, new FenetreAnticipation(90, 365));

        // Fin de contrat (CDD) : 3 mois de préavis, mais au-delà d'un mois de retard
        // le renouvellement n'est plus réalisable, l'échéance sort de la fenêtre.
        valeurs.put(TypeAnticipation.FIN_CONTRAT, new FenetreAnticipation(90, 30));

        VALEURS = Collections.unmodifiableMap(valeurs);
    }

    /** Fenêtre par défaut du type ; lève une exception pour les types non configurables. */
    public static FenetreAnticipation pour(TypeAnticipation type) {
        FenetreAnticipation fenetre = VALEURS.get(type);
        if (fenetre == null) {
            throw new IllegalArgumentException("Aucune fenêtre par défaut définie pour : " + type);
        }
        return fenetre;
    }

    /** Vrai si le type est configurable en base (donc exposable par l'API de configuration). */
    public static boolean estConfigurable(TypeAnticipation type) {
        return VALEURS.containsKey(type);
    }

    /** Les types configurables, dans l'ordre de déclaration de l'énumération. */
    public static List<TypeAnticipation> typesConfigurables() {
        return List.copyOf(VALEURS.keySet());
    }

    private FenetresParDefaut() {}
}
