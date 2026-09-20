package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Les 4 types configurables ont un couple prévenance/retard par défaut ; ANOMALIE n'en a pas. */
class FenetresParDefautTest {

    @Test
    void lesQuatreTypesConfigurablesOntUneFenetreParDefaut() {
        assertThat(FenetresParDefaut.typesConfigurables()).containsExactly(
                TypeAnticipation.DEPART_RETRAITE,
                TypeAnticipation.AVANCEMENT,
                TypeAnticipation.TITULARISATION,
                TypeAnticipation.FIN_CONTRAT);

        for (TypeAnticipation type : FenetresParDefaut.typesConfigurables()) {
            assertThat(FenetresParDefaut.estConfigurable(type)).isTrue();
            FenetreAnticipation fenetre = FenetresParDefaut.pour(type);
            assertThat(fenetre.prevenanceJours()).isPositive();
            assertThat(fenetre.retardJours()).isNotNegative();
        }
    }

    @Test
    void anomalie_nEstPasConfigurable() {
        assertThat(FenetresParDefaut.estConfigurable(TypeAnticipation.ANOMALIE)).isFalse();
        assertThatThrownBy(() -> FenetresParDefaut.pour(TypeAnticipation.ANOMALIE))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retraite_preavisLargeEtRegularisationPossible() {
        FenetreAnticipation retraite = FenetresParDefaut.pour(TypeAnticipation.DEPART_RETRAITE);

        assertThat(retraite.prevenanceJours()).isEqualTo(548); // 18 mois avant
        assertThat(retraite.contient(-365)).isTrue();          // départ dépassé : encore remonté
    }
}
