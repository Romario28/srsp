package com.entreprise.gestion.service.anticipation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Fenêtre d'affichage d'un type d'anticipation, exprimée par deux bornes en jours
 * autour de la date d'échéance :
 *
 * <pre>
 *      joursRestants &gt; 0  →  échéance à venir      (préavis)
 *      joursRestants = 0  →  échéance du jour
 *      joursRestants &lt; 0  →  échéance dépassée     (retard à régulariser)
 *
 *      ───────────┼───────────────────────────────┼──────────→
 *              -retard                           +prevenance
 *              (borne basse)                     (borne haute)
 * </pre>
 *
 * Un agent n'est visible que si {@code joursRestants} appartient à
 * {@code [-retardJours, +prevenanceJours]}.
 *
 * <ul>
 *   <li>{@code prevenanceJours} : nombre de jours <em>avant</em> l'échéance à partir
 *       duquel l'anticipation commence à être affichée (ex. 120 = prévenir 4 mois avant).</li>
 *   <li>{@code retardJours} : nombre de jours <em>après</em> l'échéance au-delà duquel
 *       l'anticipation n'est plus affichée (ex. 30 = ne montrer que les retards de moins
 *       d'un mois). {@code 0} = ne jamais afficher les échéances dépassées.</li>
 * </ul>
 *
 * Le couple effectif provient de la configuration en base si elle existe et est active,
 * sinon de {@link FenetresParDefaut}. Ce record ne connaît pas l'origine de ses valeurs :
 * c'est ce qui garantit que le batch nocturne et les endpoints API appliquent exactement
 * la même règle.
 */
public record FenetreAnticipation(int prevenanceJours, int retardJours) {

    public FenetreAnticipation {
        if (prevenanceJours < 0) {
            throw new IllegalArgumentException("La prévenance doit être positive ou nulle : " + prevenanceJours);
        }
        if (retardJours < 0) {
            throw new IllegalArgumentException("Le retard doit être positif ou nul : " + retardJours);
        }
    }

    /** Nombre de jours entre aujourd'hui et l'échéance : positif avant, négatif après. */
    public static long joursRestants(LocalDate aujourdhui, LocalDate echeance) {
        return ChronoUnit.DAYS.between(aujourdhui, echeance);
    }

    /** Borne basse de la fenêtre (négative ou nulle) : l'échéance la plus ancienne encore affichée. */
    public long borneBasse() {
        return -retardJours;
    }

    /** Borne haute de la fenêtre : l'échéance la plus lointaine encore affichée. */
    public long borneHaute() {
        return prevenanceJours;
    }

    /** Vrai si un écart en jours à l'échéance tombe dans la fenêtre. */
    public boolean contient(long joursRestants) {
        return joursRestants >= borneBasse() && joursRestants <= borneHaute();
    }

    /** Variante par dates ; une échéance absente (anomalie) est hors fenêtre. */
    public boolean contient(LocalDate aujourdhui, LocalDate echeance) {
        return echeance != null && contient(joursRestants(aujourdhui, echeance));
    }

    /**
     * Surcharge ponctuelle d'une ou des deux bornes, pour un appelant qui veut élargir
     * ou resserrer la fenêtre le temps d'une requête (paramètres {@code horizonJours} /
     * {@code prevenanceJours} / {@code retardJours}). Une borne {@code null} conserve la
     * valeur configurée.
     */
    public FenetreAnticipation surcharger(Integer prevenanceJours, Integer retardJours) {
        if (prevenanceJours == null && retardJours == null) {
            return this;
        }
        return new FenetreAnticipation(
                prevenanceJours != null ? prevenanceJours : this.prevenanceJours,
                retardJours != null ? retardJours : this.retardJours);
    }

    /** Libellé lisible, utilisé pour expliciter le motif d'affichage (ex. « [-365 j ; +548 j] »). */
    public String libelle() {
        return "[-" + retardJours + " j ; +" + prevenanceJours + " j]";
    }
}
