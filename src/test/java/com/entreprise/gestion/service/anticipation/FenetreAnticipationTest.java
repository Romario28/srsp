package com.entreprise.gestion.service.anticipation;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Bornes de la fenêtre : joursRestants positif avant l'échéance, négatif après. */
class FenetreAnticipationTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 9, 20);

    @Test
    void echeanceFutureDansLaPrevenance_estDansLaFenetre() {
        FenetreAnticipation fenetre = new FenetreAnticipation(120, 30);

        assertThat(fenetre.contient(120)).isTrue();   // borne haute incluse
        assertThat(fenetre.contient(1)).isTrue();
        assertThat(fenetre.contient(0)).isTrue();     // échéance du jour
        assertThat(fenetre.contient(121)).isFalse();  // au-delà du préavis
    }

    @Test
    void retardDansLaBorne_estDansLaFenetre() {
        FenetreAnticipation fenetre = new FenetreAnticipation(120, 30);

        assertThat(fenetre.contient(-30)).isTrue();   // borne basse incluse
        assertThat(fenetre.contient(-1)).isTrue();
        assertThat(fenetre.contient(-31)).isFalse();  // retard trop ancien
    }

    @Test
    void retardZero_neMontreQueLesEcheancesFutures() {
        FenetreAnticipation fenetre = new FenetreAnticipation(90, 0);

        assertThat(fenetre.contient(0)).isTrue();
        assertThat(fenetre.contient(90)).isTrue();
        assertThat(fenetre.contient(-1)).isFalse();
    }

    @Test
    void bornesDeLaFenetre() {
        FenetreAnticipation fenetre = new FenetreAnticipation(120, 30);

        assertThat(fenetre.borneHaute()).isEqualTo(120);
        assertThat(fenetre.borneBasse()).isEqualTo(-30);
        assertThat(fenetre.libelle()).isEqualTo("[-30 j ; +120 j]");
    }

    @Test
    void contientParDates() {
        FenetreAnticipation fenetre = new FenetreAnticipation(120, 30);

        assertThat(fenetre.contient(AUJOURDHUI, AUJOURDHUI.plusDays(120))).isTrue();
        assertThat(fenetre.contient(AUJOURDHUI, AUJOURDHUI.minusDays(30))).isTrue();
        assertThat(fenetre.contient(AUJOURDHUI, AUJOURDHUI.minusDays(31))).isFalse();
        assertThat(fenetre.contient(AUJOURDHUI, null)).isFalse(); // anomalie : pas d'échéance
    }

    @Test
    void joursRestants_estPositifAvantEtNegatifApresLEcheance() {
        assertThat(FenetreAnticipation.joursRestants(AUJOURDHUI, AUJOURDHUI.plusDays(10))).isEqualTo(10);
        assertThat(FenetreAnticipation.joursRestants(AUJOURDHUI, AUJOURDHUI.minusDays(10))).isEqualTo(-10);
    }

    @Test
    void surchargerNeModifieQueLesBornesFournies() {
        FenetreAnticipation fenetre = new FenetreAnticipation(120, 30);

        assertThat(fenetre.surcharger(null, null)).isEqualTo(fenetre);
        assertThat(fenetre.surcharger(365, null)).isEqualTo(new FenetreAnticipation(365, 30));
        assertThat(fenetre.surcharger(null, 0)).isEqualTo(new FenetreAnticipation(120, 0));
        assertThat(fenetre.surcharger(365, 0)).isEqualTo(new FenetreAnticipation(365, 0));
    }

    @Test
    void bornesNegatives_refusees() {
        assertThatThrownBy(() -> new FenetreAnticipation(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new FenetreAnticipation(0, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
