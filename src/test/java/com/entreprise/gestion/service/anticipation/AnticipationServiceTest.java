package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Sélection des agents : seul ce qui tombe dans la fenêtre [-retard ; +prévenance]
 * de son type est remonté, et les paramètres de requête ne font que surcharger la
 * fenêtre configurée.
 */
class AnticipationServiceTest {

    private static final String MATRICULE = "E00001";
    private static final LocalDate AUJOURDHUI = LocalDate.now();

    private AgentRepository agentRepository;
    private MoteurAnticipation moteur;
    private ConfigurationDelaiService configurationDelaiService;
    private AnticipationService service;
    private Agent agent;

    @BeforeEach
    void setUp() {
        agentRepository = mock(AgentRepository.class);
        moteur = mock(MoteurAnticipation.class);
        configurationDelaiService = mock(ConfigurationDelaiService.class);
        service = new AnticipationService(agentRepository, moteur, configurationDelaiService);

        agent = Agent.builder()
                .matricule(MATRICULE)
                .nom("Rakoto")
                .prenoms("Andry")
                .statut(StatutAgent.FONCTIONNAIRE)
                .build();
        when(agentRepository.findAllActifs()).thenReturn(List.of(agent));
    }

    private void fenetreConfiguree(TypeAnticipation type, int prevenanceJours, int retardJours) {
        when(configurationDelaiService.resoudrePlage(type))
                .thenReturn(new FenetreAnticipation(prevenanceJours, retardJours));
    }

    private void echeance(TypeAnticipation type, LocalDate dateEcheance) {
        when(moteur.calculerEcheances(agent)).thenReturn(List.of(
                new Echeance(MATRICULE, "Andry Rakoto", type, dateEcheance, null)));
    }

    @Test
    void echeanceDansLaPrevenance_estRemontee() {
        fenetreConfiguree(TypeAnticipation.DEPART_RETRAITE, 120, 30);
        echeance(TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI.plusDays(100));

        List<AlerteAnticipation> resultat = service.departsRetraite(null, null, null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).matricule()).isEqualTo(MATRICULE);
        assertThat(resultat.get(0).type()).isEqualTo(TypeAnticipation.DEPART_RETRAITE);
        assertThat(resultat.get(0).joursRestants()).isEqualTo(100);
    }

    @Test
    void echeanceAuDelaDeLaPrevenance_estMasquee() {
        fenetreConfiguree(TypeAnticipation.DEPART_RETRAITE, 120, 30);
        echeance(TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI.plusDays(121));

        assertThat(service.departsRetraite(null, null, null)).isEmpty();
    }

    @Test
    void retardDansLaBorne_estRemonteAvecUnJoursRestantsNegatif() {
        fenetreConfiguree(TypeAnticipation.DEPART_RETRAITE, 120, 30);
        echeance(TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI.minusDays(30));

        List<AlerteAnticipation> resultat = service.departsRetraite(null, null, null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).joursRestants()).isEqualTo(-30);
        assertThat(resultat.get(0).details()).contains("Dépassé de 1 mois");
    }

    @Test
    void retardAuDelaDeLaBorne_estMasque() {
        fenetreConfiguree(TypeAnticipation.DEPART_RETRAITE, 120, 30);
        echeance(TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI.minusDays(31));

        assertThat(service.departsRetraite(null, null, null)).isEmpty();
    }

    @Test
    void retardZero_masqueLesEcheancesDepassees() {
        fenetreConfiguree(TypeAnticipation.FIN_CONTRAT, 90, 0);
        echeance(TypeAnticipation.FIN_CONTRAT, AUJOURDHUI.minusDays(1));

        assertThat(service.finsContrat(null, null, null)).isEmpty();
    }

    @Test
    void lesParametresSurchargentLaFenetreConfiguree() {
        fenetreConfiguree(TypeAnticipation.DEPART_RETRAITE, 30, 0);
        echeance(TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI.plusDays(100));

        // Hors de la fenêtre configurée (30 j) mais dans celle demandée pour la requête.
        assertThat(service.departsRetraite(null, null, null)).isEmpty();
        assertThat(service.departsRetraite(120, null, null)).hasSize(1);
        assertThat(service.departsRetraite(null, 120, null)).hasSize(1);
    }

    @Test
    void leRetardDemandePeutElargirLaFenetre() {
        fenetreConfiguree(TypeAnticipation.AVANCEMENT, 90, 0);
        echeance(TypeAnticipation.AVANCEMENT, AUJOURDHUI.minusDays(10));

        assertThat(service.avancementsDus(null, null, null)).isEmpty();
        assertThat(service.avancementsDus(null, 30, null)).hasSize(1);
    }

    @Test
    void unAutreTypeDEcheance_nEstPasRemonte() {
        fenetreConfiguree(TypeAnticipation.DEPART_RETRAITE, 120, 30);
        when(moteur.calculerEcheances(agent)).thenReturn(List.of(
                new Echeance(MATRICULE, "Andry Rakoto", TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI.plusDays(60), null),
                new Echeance(MATRICULE, "Andry Rakoto", TypeAnticipation.AVANCEMENT, AUJOURDHUI.plusDays(10), null)));

        List<AlerteAnticipation> resultat = service.departsRetraite(null, null, null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).type()).isEqualTo(TypeAnticipation.DEPART_RETRAITE);
    }

    @Test
    void leFiltreParStatutResteApplique() {
        fenetreConfiguree(TypeAnticipation.DEPART_RETRAITE, 120, 30);
        echeance(TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI.plusDays(10));

        assertThat(service.departsRetraite(null, null, StatutAgent.FONCTIONNAIRE)).hasSize(1);
        assertThat(service.departsRetraite(null, null, StatutAgent.ELD)).isEmpty();
    }

    @Test
    void lesAnomaliesSontToujoursRemonteesSansFenetre() {
        when(moteur.calculerEcheances(agent)).thenReturn(List.of(
                new Echeance(MATRICULE, "Andry Rakoto", TypeAnticipation.ANOMALIE, null, "Grade manquant")));

        List<AlerteAnticipation> resultat = service.anomalies(null);

        assertThat(resultat).hasSize(1);
        assertThat(resultat.get(0).details()).isEqualTo("Grade manquant");
    }

    @Test
    void bornesNegatives_refusees() {
        assertThatThrownBy(() -> service.departsRetraite(-1, null, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.departsRetraite(null, -1, null))
                .isInstanceOf(BusinessException.class);
    }
}
