package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static com.entreprise.gestion.entite.anticipation.SituationIntegration.*;
import static org.assertj.core.api.Assertions.assertThat;

class RegleIntegrationTest {
    private static Agent agent(StatutAgent statut, LocalDate debut, LocalDate fin) {
        return Agent.builder().matricule("123456").prenoms("Jean").nom("Rakoto")
                .statut(statut).dateDebutContrat(debut).dateFinContrat(fin).build();
    }
    @Test void contractuelSansDateDeFinAvecDebutEstConcerne() {
        var a=RegleIntegration.analyser(agent(StatutAgent.CONTRACTUEL,LocalDate.of(2024,10,15),null));
        assertThat(a.situation()).isEqualTo(CONCERNE);
        assertThat(a.dateIntegration()).isEqualTo(LocalDate.of(2026,10,15));
    }
    @Test void contractuelAvecDateDeFinNEstPasConcerne() {
        var a=RegleIntegration.analyser(agent(StatutAgent.CONTRACTUEL,LocalDate.of(2024,10,15),LocalDate.of(2027,1,1)));
        assertThat(a.situation()).isEqualTo(CONTRAT_AVEC_FIN); assertThat(a.dateIntegration()).isNull();
    }
    @Test void contractuelSansDateDeDebutEstNonCalculable() {
        var a=RegleIntegration.analyser(agent(StatutAgent.CONTRACTUEL,null,null));
        assertThat(a.situation()).isEqualTo(DEBUT_MANQUANT); assertThat(a.dateIntegration()).isNull();
    }
    @Test void uneDateDeFinPrimeSurUnDebutManquant() {
        assertThat(RegleIntegration.analyser(agent(StatutAgent.CONTRACTUEL,null,LocalDate.of(2027,1,1))).situation()).isEqualTo(CONTRAT_AVEC_FIN);
    }
    @Test void seulsLesContractuelsSontConcernes() {
        for(StatutAgent statut:new StatutAgent[]{StatutAgent.FONCTIONNAIRE,StatutAgent.ELD,null})
            assertThat(RegleIntegration.analyser(agent(statut,LocalDate.of(2024,10,15),null)).situation()).as("statut %s",statut).isEqualTo(NON_CONTRACTUEL);
    }
    @Test void le29FevrierRetombeSurLe28() {
        assertThat(RegleIntegration.analyser(agent(StatutAgent.CONTRACTUEL,LocalDate.of(2024,2,29),null)).dateIntegration()).isEqualTo(LocalDate.of(2026,2,28));
    }
}
