package com.entreprise.gestion.service.anticipation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FenetreAnticipationTest {

    private final LocalDate aujourdhui = LocalDate.of(2026, 10, 5);

    @Test
    void datesPreparationEtFinToleranceSontCalculeesDepuisEcheance() {
        var fenetre = new FenetreAnticipation(18, 6);
        LocalDate echeance = LocalDate.of(2027, 10, 5);
        assertThat(fenetre.datePreparation(echeance)).isEqualTo(LocalDate.of(2026, 4, 5));
        assertThat(fenetre.dateFinTolerance(echeance)).isEqualTo(LocalDate.of(2028, 4, 5));
    }

    @Test
    void bornesInclusesEtRetardZeroGardeLeJour() {
        var fenetre = new FenetreAnticipation(3, 0);
        LocalDate echeance = aujourdhui.plusMonths(3);
        assertThat(fenetre.contient(echeance, aujourdhui)).isTrue();
        assertThat(fenetre.contient(echeance.plusDays(1), aujourdhui)).isFalse();
        assertThat(fenetre.contient(aujourdhui, aujourdhui)).isTrue();
        assertThat(fenetre.contient(aujourdhui.minusDays(1), aujourdhui)).isFalse();
    }

    @Test
    void finDeMoisRamenéeAuDernierJourValide() {
        var fenetre = new FenetreAnticipation(1, 0);
        assertThat(fenetre.datePreparation(LocalDate.of(2026, 2, 28)))
                .isEqualTo(LocalDate.of(2026, 1, 28));
        assertThat(fenetre.dateFinTolerance(LocalDate.of(2026, 1, 31)))
                .isEqualTo(LocalDate.of(2026, 2, 28));
    }
}
