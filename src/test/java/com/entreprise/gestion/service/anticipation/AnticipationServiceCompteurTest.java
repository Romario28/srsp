package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Ministere;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnomalie;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.service.acces.Perimetre;
import com.entreprise.gestion.service.acces.PerimetreCourant;
import com.entreprise.gestion.service.dossier.SuiviDossiers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnticipationServiceCompteurTest {
    @Mock AgentRepository agentRepository;
    @Mock MoteurAnticipation moteur;
    @Mock ConfigurationDelaiService configurationDelaiService;
    @Mock GradeSuivantService gradeSuivantService;
    @Mock SuiviDossiers suiviDossiers;
    @Mock PerimetreCourant perimetreCourant;
    @InjectMocks AnticipationService service;

    private static Agent agent(String matricule, String ministere) {
        return Agent.builder().matricule(matricule).statut(StatutAgent.FONCTIONNAIRE)
                .ministere(Ministere.builder().code(ministere).build()).build();
    }
    private static Echeance anomalie(String matricule, TypeAnomalie type) {
        return new Echeance(matricule, "Jean " + matricule, TypeAnticipation.ANOMALIE, null, type.libelle(), type);
    }

    @Test void neCompteQueLesAnomaliesDuPerimetre() {
        Agent dedans = agent("D", "05"), dehors = agent("H", "06");
        when(perimetreCourant.get()).thenReturn(new Perimetre(false, Set.of("05"), Set.of(), Set.of()));
        when(agentRepository.findAllActifs()).thenReturn(List.of(dedans, dehors));
        when(moteur.calculerEcheances(dedans)).thenReturn(List.of(
                new Echeance("D", "Jean D", TypeAnticipation.DEPART_RETRAITE, LocalDate.of(2030, 1, 1), null),
                anomalie("D", TypeAnomalie.GRADE_MANQUANT),
                anomalie("D", TypeAnomalie.CORPS_MANQUANT)));

        assertThat(service.compterAnomalies()).isEqualTo(2);   // les échéances normales ne comptent pas
        verify(moteur, never()).calculerEcheances(dehors);     // l'agent hors périmètre n'est même pas calculé
    }

    @Test void unCompteSansPerimetreNeVoitAucuneAnomalie() {
        Agent a = agent("A", "05");
        when(perimetreCourant.get()).thenReturn(Perimetre.aucun());
        when(agentRepository.findAllActifs()).thenReturn(List.of(a));

        assertThat(service.compterAnomalies()).isZero();
        verify(moteur, never()).calculerEcheances(a);
    }
}
