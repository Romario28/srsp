package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnomalie;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;

class MoteurAnticipationIntegrationTest {
    private final MoteurAnticipation moteur=new MoteurAnticipation(null,null);
    private static Agent contractuel(LocalDate debut,LocalDate fin) {
        return Agent.builder().matricule("123456").prenoms("Jean").nom("Rakoto").statut(StatutAgent.CONTRACTUEL).dateDebutContrat(debut).dateFinContrat(fin).build();
    }
    @Test void dateDeFinVideUneEcheanceIntegration() {
        Echeance e=moteur.calculerIntegration(contractuel(LocalDate.of(2024,10,15),null));
        assertThat(e.type()).isEqualTo(TypeAnticipation.INTEGRATION); assertThat(e.dateEcheance()).isEqualTo(LocalDate.of(2026,10,15)); assertThat(e.typeAnomalie()).isNull();
    }
    @Test void dateDeFinRenseigneeAucuneEcheanceNiAnomalie() { assertThat(moteur.calculerIntegration(contractuel(LocalDate.of(2024,10,15),LocalDate.of(2027,1,1)))).isNull(); }
    @Test void sansDateDeDebutUneAnomalie() {
        Echeance e=moteur.calculerIntegration(contractuel(null,null));
        assertThat(e.type()).isEqualTo(TypeAnticipation.ANOMALIE); assertThat(e.typeAnomalie()).isEqualTo(TypeAnomalie.DEBUT_CONTRAT_MANQUANT); assertThat(e.dateEcheance()).isNull();
    }
    @Test void sansDateDeDebutMaisAvecDateDeFinNiEcheanceNiAnomalie() { assertThat(moteur.calculerIntegration(contractuel(null,LocalDate.of(2027,1,1)))).isNull(); }
    @Test void fenetreParDefautPreparationTroisMoisToleranceZero() {
        FenetreAnticipation f=DelaisParDefaut.pour(TypeAnticipation.INTEGRATION); LocalDate date=LocalDate.of(2026,10,15);
        assertThat(f).isEqualTo(new FenetreAnticipation(3,0)); assertThat(f.datePreparation(date)).isEqualTo(LocalDate.of(2026,7,15));
        assertThat(f.contient(date,LocalDate.of(2026,7,14))).isFalse(); assertThat(f.contient(date,LocalDate.of(2026,7,15))).isTrue();
        assertThat(f.contient(date,date)).isTrue(); assertThat(f.contient(date,LocalDate.of(2026,10,16))).isFalse();
    }
}
