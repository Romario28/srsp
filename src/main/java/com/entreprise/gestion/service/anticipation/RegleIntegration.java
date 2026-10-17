package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.SituationIntegration;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import java.time.LocalDate;

/** Règle métier d'intégration, partagée par le moteur et la fiche agent. */
public final class RegleIntegration {
    public static final int DUREE_ANS = 2;
    public record Analyse(SituationIntegration situation, LocalDate dateIntegration) {}

    public static Analyse analyser(Agent a) {
        if (a.getStatut() != StatutAgent.CONTRACTUEL) return new Analyse(SituationIntegration.NON_CONTRACTUEL, null);
        if (a.getDateFinContrat() != null) return new Analyse(SituationIntegration.CONTRAT_AVEC_FIN, null);
        if (a.getDateDebutContrat() == null) return new Analyse(SituationIntegration.DEBUT_MANQUANT, null);
        return new Analyse(SituationIntegration.CONCERNE, a.getDateDebutContrat().plusYears(DUREE_ANS));
    }
    private RegleIntegration() {}
}
