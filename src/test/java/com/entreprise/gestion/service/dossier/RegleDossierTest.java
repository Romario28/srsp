package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class RegleDossierTest {
    private final LocalDate auj = LocalDate.of(2026, 10, 16);

    private static Dossier dossier(StatutDossier statut, LocalDate echeance, LocalDate demarrage) {
        return Dossier.builder().type(TypeAnticipation.AVANCEMENT).datePreparation(echeance.minusMonths(3))
                .dateEcheance(echeance).statut(statut).dateDemarrage(demarrage).build();
    }

    @Test void ilNExisteQueTroisStatuts() {
        assertThat(StatutDossier.values()).extracting(Enum::name)
                .containsExactlyInAnyOrder("EN_ATTENTE_DE_TRAITEMENT", "EN_COURS", "SANS_SUITE");
    }

    @Test void etatAffiche() {
        assertThat(RegleDossier.etat(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, auj, auj)).isEqualTo(EtatDossier.A_TRAITER);
        assertThat(RegleDossier.etat(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, auj.minusDays(1), auj)).isEqualTo(EtatDossier.EN_RETARD);
        assertThat(RegleDossier.etat(StatutDossier.EN_COURS, auj.minusMonths(2), auj)).isEqualTo(EtatDossier.EN_COURS);
        assertThat(RegleDossier.etat(StatutDossier.SANS_SUITE, auj.minusMonths(2), auj)).isEqualTo(EtatDossier.SANS_SUITE);
    }

    @Test void enRetardSiNonDemarreEtEcheanceDepassee() {
        assertThat(RegleDossier.enRetard(dossier(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, auj.minusDays(1), null), auj)).isTrue();
    }
    @Test void leJourDeLEcheanceLeDossierEstEncoreATraiter() {
        assertThat(RegleDossier.enRetard(dossier(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, auj, null), auj)).isFalse();
    }
    @Test void preparationDepasseeMaisEcheanceAVenirNEstPasEnRetard() {
        assertThat(RegleDossier.enRetard(dossier(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, auj.plusMonths(2), null), auj)).isFalse();
    }
    @Test void unDossierEnCoursNEstJamaisEnRetard() {
        assertThat(RegleDossier.enRetard(dossier(StatutDossier.EN_COURS, auj.minusMonths(6), auj.minusMonths(5)), auj)).isFalse();
    }
    @Test void unDossierSansSuiteNEstJamaisEnRetard() {
        assertThat(RegleDossier.enRetard(dossier(StatutDossier.SANS_SUITE, auj.minusMonths(6), null), auj)).isFalse();
    }
    @Test void reouvertureRestitueLEtatDAvantAbandon() {
        assertThat(RegleDossier.statutApresReouverture(dossier(StatutDossier.SANS_SUITE, auj, auj.minusDays(3)))).isEqualTo(StatutDossier.EN_COURS);
        assertThat(RegleDossier.statutApresReouverture(dossier(StatutDossier.SANS_SUITE, auj, null))).isEqualTo(StatutDossier.EN_ATTENTE_DE_TRAITEMENT);
    }
}
