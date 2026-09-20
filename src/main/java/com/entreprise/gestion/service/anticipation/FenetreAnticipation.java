package com.entreprise.gestion.service.anticipation;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Fenêtre d'anticipation à deux bornes, exprimée en jours par rapport à l'échéance.
 *
 * <ul>
 *   <li>{@code prevenanceJours} — « plus tôt » : nombre de jours AVANT l'échéance à partir
 *       desquels l'anticipation commence à être affichée (120 → prévenir 4 mois avant).</li>
 *   <li>{@code retardJours} — « plus tard » : nombre de jours APRÈS l'échéance dépassée
 *       jusqu'auxquels l'anticipation reste affichée (30 → au maximum 1 mois de retard).
 *       {@code 0} → les échéances dépassées ne sont jamais affichées.</li>
 * </ul>
 *
 * Un agent n'est visible que si {@code joursRestants ∈ [-retardJours, +prevenanceJours]}.
 * Le couple effectif vient de la base si une configuration active existe pour le type,
 * sinon de {@link FenetresParDefaut} (voir {@link ConfigurationDelaiService}).
 */
public record FenetreAnticipation(int prevenanceJours, int retardJours) {

    public FenetreAnticipation {
        if (prevenanceJours < 0) {
            throw new IllegalArgumentException("La prévenance doit être positive ou nulle.");
        }
        if (retardJours < 0) {
            throw new IllegalArgumentException("Le retard doit être positif ou nul.");
        }
    }

    /**
     * Nombre de jours restants avant l'échéance, tel qu'affiché :
     * positif pour une échéance à venir, négatif pour une échéance dépassée, 0 le jour J.
     * Unique formule utilisée par le batch et par les endpoints API.
     */
    public static long joursRestants(LocalDate reference, LocalDate dateEcheance) {
        return ChronoUnit.DAYS.between(reference, dateEcheance);
    }

    /** L'échéance tombe-t-elle dans la fenêtre (- retard … + prévenance) ? */
    public boolean contient(long joursRestants) {
        return joursRestants >= -retardJours && joursRestants <= prevenanceJours;
    }

    /** Copie dont seule la borne de prévenance change (override ponctuel d'un appelant). */
    public FenetreAnticipation avecPrevenance(int nouvellePrevenanceJours) {
        return new FenetreAnticipation(nouvellePrevenanceJours, retardJours);
    }

    /** Copie dont seule la borne de retard change (override ponctuel d'un appelant). */
    public FenetreAnticipation avecRetard(int nouveauRetardJours) {
        return new FenetreAnticipation(prevenanceJours, nouveauRetardJours);
    }
}
