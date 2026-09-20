package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Règles de la fenêtre à deux bornes : [-retard, +prévenance]. */
class FenetreAnticipationTest {

    private static final FenetreAnticipation FENETRE = new FenetreAnticipation(120, 30);

    @Test
    @DisplayName("joursRestants : positif à venir, négatif en retard, 0 le jour J")
    void joursRestantsSigne() {
        LocalDate reference = LocalDate.of(2026, 9, 20);

        assertEquals(10, FenetreAnticipation.joursRestants(reference, reference.plusDays(10)));
        assertEquals(-10, FenetreAnticipation.joursRestants(reference, reference.minusDays(10)));
        assertEquals(0, FenetreAnticipation.joursRestants(reference, reference));
    }

    @Test
    @DisplayName("Visibilité bornée par la prévenance et le retard")
    void contient() {
        assertTrue(FENETRE.contient(120), "exactement à la borne de prévenance");
        assertTrue(FENETRE.contient(119));
        assertTrue(FENETRE.contient(0), "échéance du jour");
        assertTrue(FENETRE.contient(-30), "exactement à la borne de retard");
        assertFalse(FENETRE.contient(121), "trop tôt : au-delà de la prévenance");
        assertFalse(FENETRE.contient(-31), "trop tard : au-delà du retard");
    }

    @Test
    @DisplayName("Retard 0 : les échéances dépassées ne sont jamais affichées")
    void retardZero() {
        FenetreAnticipation sansRetard = new FenetreAnticipation(120, 0);

        assertTrue(sansRetard.contient(0));
        assertFalse(sansRetard.contient(-1));
        assertFalse(sansRetard.contient(-30));
    }

    @Test
    @DisplayName("Modification d'une seule borne")
    void bornesModifiables() {
        assertEquals(new FenetreAnticipation(60, 30), FENETRE.avecPrevenance(60));
        assertEquals(new FenetreAnticipation(120, 0), FENETRE.avecRetard(0));
    }

    @Test
    @DisplayName("Bornes négatives refusées")
    void bornesNegativesRefusees() {
        assertThrows(IllegalArgumentException.class, () -> new FenetreAnticipation(-1, 30));
        assertThrows(IllegalArgumentException.class, () -> new FenetreAnticipation(120, -1));
    }

    @Test
    @DisplayName("Défauts : un couple par type métier, aucun pour ANOMALIE")
    void defauts() {
        assertEquals(new FenetreAnticipation(548, 365), FenetresParDefaut.pour(TypeAnticipation.DEPART_RETRAITE));
        assertEquals(new FenetreAnticipation(90, 30), FenetresParDefaut.pour(TypeAnticipation.AVANCEMENT));
        assertEquals(new FenetreAnticipation(90, 30), FenetresParDefaut.pour(TypeAnticipation.TITULARISATION));
        assertEquals(new FenetreAnticipation(90, 30), FenetresParDefaut.pour(TypeAnticipation.FIN_CONTRAT));

        assertTrue(FenetresParDefaut.configurable(TypeAnticipation.FIN_CONTRAT));
        assertFalse(FenetresParDefaut.configurable(TypeAnticipation.ANOMALIE));
        assertThrows(IllegalArgumentException.class, () -> FenetresParDefaut.pour(TypeAnticipation.ANOMALIE));
    }
}
