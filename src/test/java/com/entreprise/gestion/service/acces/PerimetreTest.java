package com.entreprise.gestion.service.acces;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PerimetreTest {
    private static Perimetre p(Set<String> ministeres, Set<String> soas, Set<String> fivs) {
        return new Perimetre(false, ministeres, soas, fivs);
    }

    @Test void unComptePlusPetitQueLeChefEstCouvert() {
        assertThat(p(Set.of("05"), Set.of(), Set.of()).couvre(p(Set.of("05"), Set.of(), Set.of("L01")))).isTrue();
    }
    @Test void unCompteQuiDebordeNEstPasCouvert() {
        assertThat(p(Set.of("05"), Set.of(), Set.of()).couvre(p(Set.of("05", "06"), Set.of(), Set.of()))).isFalse();
    }
    @Test void unCompteSansRestrictionSurUnAxeRestreintParLeChefNEstPasCouvert() {
        assertThat(p(Set.of("05"), Set.of(), Set.of()).couvre(p(Set.of(), Set.of(), Set.of("L01")))).isFalse();
    }
    @Test void unAxeLibreDuChefNeRestreintPas() {
        assertThat(p(Set.of("05"), Set.of(), Set.of()).couvre(p(Set.of("05"), Set.of("S1"), Set.of()))).isTrue();
    }
    @Test void unCompteSansAucuneValeurNEstRattacheAPersonne() {
        assertThat(p(Set.of("05"), Set.of(), Set.of()).couvre(Perimetre.aucun())).isFalse();
    }
    @Test void unAccesTotalCouvreTout() {
        assertThat(Perimetre.tout().couvre(p(Set.of("06"), Set.of(), Set.of()))).isTrue();
    }
    @Test void lesParametresDeRequeteNeSontJamaisVides() {
        Perimetre libre = Perimetre.tout();
        assertThat(libre.restreint()).isFalse();
        assertThat(libre.ministeresPourRequete()).isNotEmpty();
        assertThat(p(Set.of("05"), Set.of(), Set.of()).restreintMinisteres()).isTrue();
    }
}
