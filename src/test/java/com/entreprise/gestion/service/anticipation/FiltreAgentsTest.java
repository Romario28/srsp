package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FiltreAgentsTest {

    private final Agent agent = Agent.builder().matricule("123456").statut(StatutAgent.ELD)
            .corps(Corps.builder().code("A88J").categorie("10").build())
            .grade(Grade.builder().code("2C1E").build())
            .localite(Localite.builder().code("L01").build())
            .soa(Soa.builder().code("S1").build())
            .ministere(Ministere.builder().code("05").build())
            .regCode("R3").build();

    @Test void sansFiltreToutEstAccepte() {
        assertThat(FiltreAgents.AUCUN.estVide()).isTrue();
        assertThat(FiltreAgents.AUCUN.accepte(agent)).isTrue();
        assertThat(FiltreAgents.AUCUN.accepte(new Agent())).isTrue();
    }

    @Test void chaqueCritereFiltre() {
        assertThat(new FiltreAgents(StatutAgent.ELD, null, null, null, null, null, null, null).accepte(agent)).isTrue();
        assertThat(new FiltreAgents(StatutAgent.CONTRACTUEL, null, null, null, null, null, null, null).accepte(agent)).isFalse();
        assertThat(new FiltreAgents(null, "A88J", null, null, null, null, null, null).accepte(agent)).isTrue();
        assertThat(new FiltreAgents(null, "A88L", null, null, null, null, null, null).accepte(agent)).isFalse();
        assertThat(new FiltreAgents(null, null, "2C1E", null, null, null, null, null).accepte(agent)).isTrue();
        assertThat(new FiltreAgents(null, null, null, "10", null, null, null, null).accepte(agent)).isTrue();
        assertThat(new FiltreAgents(null, null, null, "09", null, null, null, null).accepte(agent)).isFalse();
        assertThat(new FiltreAgents(null, null, null, null, "L01", null, null, null).accepte(agent)).isTrue();
        assertThat(new FiltreAgents(null, null, null, null, null, "S1", null, null).accepte(agent)).isTrue();
        assertThat(new FiltreAgents(null, null, null, null, null, null, "R3", null).accepte(agent)).isTrue();
        assertThat(new FiltreAgents(null, null, null, null, null, null, null, "05").accepte(agent)).isTrue();
        assertThat(new FiltreAgents(null, null, null, null, null, null, null, "06").accepte(agent)).isFalse();
    }

    @Test void lesFiltresSontCumulatifs() {
        assertThat(new FiltreAgents(StatutAgent.ELD, "A88J", "2C1E", "10", "L01", "S1", "R3", "05").accepte(agent)).isTrue();
        assertThat(new FiltreAgents(StatutAgent.ELD, "A88J", "AUTRE", null, null, null, null, null).accepte(agent)).isFalse();
    }

    @Test void unAttributAbsentNeCorrespondAAucunFiltre() {
        Agent vide = new Agent();
        assertThat(new FiltreAgents(null, "A88J", null, null, null, null, null, null).accepte(vide)).isFalse();
        assertThat(new FiltreAgents(null, null, null, null, "L01", null, null, null).accepte(vide)).isFalse();
        assertThat(new FiltreAgents(null, null, null, null, null, null, "R3", null).accepte(vide)).isFalse();
    }

    @Test void uneValeurVideOuBlancheEstIgnoree() {
        FiltreAgents f = new FiltreAgents(null, "  ", "", null, null, null, null, null);
        assertThat(f.estVide()).isTrue();
        assertThat(f.accepte(agent)).isTrue();
    }
}
