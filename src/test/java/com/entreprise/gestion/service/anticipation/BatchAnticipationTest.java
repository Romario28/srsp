package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Alerte;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.anticipation.AlerteRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Le batch nocturne applique exactement la même fenêtre que les endpoints API :
 * rien n'est persisté hors de l'intervalle [-retard, +prévenance], et les anomalies
 * restent toujours remontées.
 */
class BatchAnticipationTest {

    private final AgentRepository agentRepository = mock(AgentRepository.class);
    private final MoteurAnticipation moteur = mock(MoteurAnticipation.class);
    private final AlerteRepository alerteRepository = mock(AlerteRepository.class);
    private final ConfigurationDelaiService configurationDelaiService = mock(ConfigurationDelaiService.class);
    private final EntityManager entityManager = mock(EntityManager.class);

    private final BatchAnticipation batch = new BatchAnticipation(
            agentRepository, moteur, alerteRepository, configurationDelaiService, entityManager);

    private final Agent agent = Agent.builder().matricule("M1").nom("Nom").prenoms("Prénom").build();

    @Test
    @DisplayName("Seules les échéances dans [-retard, +prévenance] créent une alerte")
    void fenetreDeuxBornes() {
        LocalDate aujourdhui = LocalDate.now();
        preparer(aujourdhui, new FenetreAnticipation(90, 30), List.of(
                echeance(aujourdhui.plusDays(91)),                                       // trop tôt
                echeance(aujourdhui.plusDays(90)),                                       // borne de prévenance
                echeance(aujourdhui.minusDays(30)),                                      // borne de retard
                echeance(aujourdhui.minusDays(31)),                                      // trop tard
                new Echeance("M1", "Prénom Nom", TypeAnticipation.ANOMALIE, null, "Grade manquant")));

        batch.executer();

        ArgumentCaptor<Alerte> captor = ArgumentCaptor.forClass(Alerte.class);
        verify(alerteRepository, times(3)).save(captor.capture());
        List<Alerte> enregistrees = captor.getAllValues();

        assertEquals(aujourdhui.plusDays(90), enregistrees.get(0).getDateEcheance());
        assertEquals(aujourdhui.minusDays(30), enregistrees.get(1).getDateEcheance());
        assertEquals(TypeAnticipation.ANOMALIE, enregistrees.get(2).getType());
        assertNull(enregistrees.get(2).getDateEcheance());
    }

    @Test
    @DisplayName("Retard configuré à 0 : aucune échéance dépassée n'est persistée")
    void retardZero() {
        LocalDate aujourdhui = LocalDate.now();
        preparer(aujourdhui, new FenetreAnticipation(90, 0), List.of(
                echeance(aujourdhui.minusDays(1)),
                echeance(aujourdhui)));

        batch.executer();

        ArgumentCaptor<Alerte> captor = ArgumentCaptor.forClass(Alerte.class);
        verify(alerteRepository, times(1)).save(captor.capture());
        assertEquals(aujourdhui, captor.getValue().getDateEcheance());
    }

    @Test
    @DisplayName("La fenêtre du batch vient de la configuration résolue (base ou défaut), pas d'une constante")
    void fenetreResolueUneFois() {
        LocalDate aujourdhui = LocalDate.now();
        preparer(aujourdhui, new FenetreAnticipation(5, 5), List.of(echeance(aujourdhui.plusDays(6))));

        batch.executer();

        verify(configurationDelaiService, times(1)).resoudreToutes();
        verify(alerteRepository, never()).save(any(Alerte.class));
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private void preparer(LocalDate aujourdhui, FenetreAnticipation fenetre, List<Echeance> echeances) {
        when(configurationDelaiService.resoudreToutes())
                .thenReturn(Map.of(TypeAnticipation.AVANCEMENT, fenetre));
        when(agentRepository.findAllActifs()).thenReturn(List.of(agent));
        when(moteur.calculerEcheances(agent)).thenReturn(echeances);
        when(alerteRepository.findByMatriculeAgentAndTypeAndStatutNot(any(), any(), any()))
                .thenReturn(List.of());
        when(alerteRepository.existsByMatriculeAgentAndTypeAndStatutAndDateEcheance(any(), any(), any(), any()))
                .thenReturn(false);
    }

    private static Echeance echeance(LocalDate date) {
        return new Echeance("M1", "Prénom Nom", TypeAnticipation.AVANCEMENT, date, null);
    }
}
