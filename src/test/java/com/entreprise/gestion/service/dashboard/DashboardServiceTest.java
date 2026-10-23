package com.entreprise.gestion.service.dashboard;

import com.entreprise.gestion.dto.dashboard.EcheancesAVenirDTO;
import com.entreprise.gestion.dto.dashboard.EffectifsDTO;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Corps;
import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.entite.anticipation.Ministere;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.service.anticipation.MoteurAnticipation;
import com.entreprise.gestion.service.anticipation.Echeance;
import com.entreprise.gestion.service.anticipation.ConfigurationDelaiService;
import com.entreprise.gestion.service.anticipation.FenetreAnticipation;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 10, 20);

    @Mock AgentRepository agentRepository;
    @Mock MoteurAnticipation moteur;
    @Mock ConfigurationDelaiService configurationDelaiService;
    @InjectMocks DashboardService service;

    @BeforeEach
    void configurerAgeLegal() {
        lenient().when(moteur.ageRetraite()).thenReturn(60);
        lenient().when(moteur.dateRetraite(any())).thenAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            return agent.getDateNaissance() == null ? null : agent.getDateNaissance().plusYears(60);
        });
        lenient().when(configurationDelaiService.resoudreToutes()).thenReturn(Map.of());
    }

    private EffectifsDTO effectifs(LocalDate jour) { return service.effectifs(jour, FiltreAgents.AUCUN); }

    private static Agent agentDuMinistere(String matricule, String code) {
        Agent.AgentBuilder builder = Agent.builder().matricule(matricule).statut(StatutAgent.FONCTIONNAIRE);
        if (code != null) builder.ministere(Ministere.builder().code(code).build());
        return builder.build();
    }

    private static Agent agent(String matricule, StatutAgent statut, LocalDate naissance, String sexe, String categorie) {
        Agent.AgentBuilder builder = Agent.builder().matricule(matricule).statut(statut)
                .dateNaissance(naissance).sexe(sexe);
        if (categorie != null) builder.corps(Corps.builder().code("C1").categorie(categorie).build());
        return builder.build();
    }

    @Test
    void effectifs_compteLesStatutsEtLesTranchesAuxBornes() {
        Agent age25 = agent("A", StatutAgent.FONCTIONNAIRE, LocalDate.of(2001, 10, 20), "M", "I");
        Agent age24 = agent("B", StatutAgent.CONTRACTUEL, LocalDate.of(2001, 10, 21), "F", "I");
        Agent age59 = agent("C", StatutAgent.ELD, LocalDate.of(1966, 10, 21), "X", null);
        Agent age60 = agent("D", null, LocalDate.of(1966, 10, 20), null, "II");
        Agent sansNaissance = agent("E", null, null, "F", null);
        when(agentRepository.findAllActifs()).thenReturn(List.of(age25, age24, age59, age60, sansNaissance));

        EffectifsDTO dto = effectifs(AUJOURDHUI);

        assertThat(dto.total()).isEqualTo(5);
        assertThat(dto.fonctionnaires()).isEqualTo(1);
        assertThat(dto.contractuels()).isEqualTo(1);
        assertThat(dto.eld()).isEqualTo(1);
        assertThat(dto.statutNonRenseigne()).isEqualTo(2);
        assertThat(dto.ages().sansDateNaissance()).isEqualTo(1);
        assertThat(dto.ages().ageLegal()).isEqualTo(60);
        assertThat(dto.ages().lignes()).anySatisfy(l -> {
            assertThat(l.tranche()).isZero(); assertThat(l.sexe()).isEqualTo("F"); assertThat(l.nombre()).isEqualTo(1);
        });
        assertThat(dto.ages().lignes()).anySatisfy(l -> assertThat(l.tranche()).isEqualTo(1)); // 25–29
        assertThat(dto.ages().lignes()).anySatisfy(l -> assertThat(l.tranche()).isEqualTo(7)); // 55–59
        assertThat(dto.ages().lignes()).anySatisfy(l -> assertThat(l.tranche()).isEqualTo(8)); // 60–64
    }

    @Test
    void ageLeJourDeLAnniversaireDu29FevrierEstCompteEnAnneesRevolues() {
        LocalDate jour = LocalDate.of(2021, 2, 28);
        Agent anniversaire = agent("LEAP", StatutAgent.FONCTIONNAIRE, LocalDate.of(1996, 2, 29), "F", null);
        when(agentRepository.findAllActifs()).thenReturn(List.of(anniversaire));

        EffectifsDTO dto = effectifs(jour);

        assertThat(dto.ages().lignes()).singleElement()
                .satisfies(ligne -> assertThat(ligne.tranche()).isEqualTo(1)); // 25 ans, tranche 25–29
    }

    @Test
    void ageEtProjectionTraitentAnniversairesEt29Fevrier() {
        Agent anniversaire = agent("A", StatutAgent.FONCTIONNAIRE, LocalDate.of(1966, 10, 20), "M", null);
        Agent lendemain = agent("B", StatutAgent.FONCTIONNAIRE, LocalDate.of(1966, 10, 21), "F", null);
        Agent fevrier = agent("C", StatutAgent.FONCTIONNAIRE, LocalDate.of(1964, 2, 29), "M", null);
        Agent retraiteAtteinte = agent("D", StatutAgent.FONCTIONNAIRE, LocalDate.of(1965, 9, 1), "M", null);
        Agent retraiteAnneeSuivante = agent("E", StatutAgent.FONCTIONNAIRE, LocalDate.of(1967, 11, 1), "F", null);
        when(agentRepository.findAllActifs()).thenReturn(List.of(anniversaire, lendemain, fevrier, retraiteAtteinte, retraiteAnneeSuivante));

        EffectifsDTO dto = effectifs(AUJOURDHUI);

        assertThat(dto.retraites().depuis()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(dto.retraites().mois()).hasSize(12);
        assertThat(dto.retraites().mois().get(0).cle()).isEqualTo("2026-10");
        assertThat(dto.retraites().mois().get(0).nombre()).isEqualTo(2);
        assertThat(dto.retraites().annees()).hasSize(5);
        assertThat(dto.retraites().annees().get(1).cle()).isEqualTo("2027");
        assertThat(dto.retraites().annees().get(1).nombre()).isEqualTo(1);
        assertThat(dto.retraites().dejaAtteint()).isEqualTo(2);
    }

    @Test
    void echeancesCompteLesDeuxBornesEtExclutAnomalies() {
        Agent agent = agent("A", StatutAgent.ELD, null, "M", null);
        when(agentRepository.findAllActifs()).thenReturn(List.of(agent));
        when(moteur.calculerEcheances(agent)).thenReturn(List.of(
                echeance(TypeAnticipation.AVANCEMENT, AUJOURDHUI.minusDays(1)),
                echeance(TypeAnticipation.DEPART_RETRAITE, AUJOURDHUI),
                echeance(TypeAnticipation.FIN_CONTRAT, AUJOURDHUI.plusDays(29)),
                echeance(TypeAnticipation.TITULARISATION, AUJOURDHUI.plusDays(30)),
                new Echeance("A", "Agent", TypeAnticipation.ANOMALIE, null, "anomalie")));

        EcheancesAVenirDTO dto = service.echeances(30, AUJOURDHUI);

        assertThat(dto.du()).isEqualTo(AUJOURDHUI);
        assertThat(dto.au()).isEqualTo(AUJOURDHUI.plusDays(29));
        assertThat(dto.total()).isEqualTo(2);
        assertThat(dto.parCategorie().get(com.entreprise.gestion.entite.anticipation.CategorieCalendrier.DEPART_RETRAITE)).isEqualTo(1);
        assertThat(dto.parCategorie().get(com.entreprise.gestion.entite.anticipation.CategorieCalendrier.FIN_CONTRAT)).isEqualTo(1);
        assertThat(dto.parCategorie().get(com.entreprise.gestion.entite.anticipation.CategorieCalendrier.ELD)).isZero();
    }

    @Test
    void echeancesRefuseUneFenetreInvalide() {
        assertThatThrownBy(() -> service.echeances(0, AUJOURDHUI)).hasMessageContaining("compris entre 1");
    }

    @Test
    void perimetreRestreintEffectifsEtEcheancesEtEviteLeCalculHorsPerimetre() {
        Agent dedans = agentDuMinistere("D", "05");
        Agent dehors = agentDuMinistere("H", "06");
        Agent sansMinistere = agentDuMinistere("S", null);
        when(agentRepository.findAllActifs()).thenReturn(List.of(dedans, dehors, sansMinistere));
        when(moteur.calculerEcheances(dedans)).thenReturn(List.of(echeance(TypeAnticipation.FIN_CONTRAT, AUJOURDHUI.plusDays(4))));
        FiltreAgents portee = new FiltreAgents(null, null, null, null, null, null, null, "05");

        assertThat(service.effectifs(AUJOURDHUI, portee).total()).isEqualTo(1);
        assertThat(service.echeances(30, AUJOURDHUI, portee).total()).isEqualTo(1);
        verify(moteur, never()).calculerEcheances(dehors);
        verify(moteur, never()).calculerEcheances(sansMinistere);
    }

    @Test
    void echeancesComptePreparationEtMoisEtAnneeCivilsAvecBornesInclusives() {
        Agent agent = agent("A", StatutAgent.FONCTIONNAIRE, null, "M", null);
        when(agentRepository.findAllActifs()).thenReturn(List.of(agent));
        when(configurationDelaiService.resoudreToutes()).thenReturn(Map.of(
                TypeAnticipation.DEPART_RETRAITE, new FenetreAnticipation(12, 0),
                TypeAnticipation.AVANCEMENT, new FenetreAnticipation(3, 0),
                TypeAnticipation.INTEGRATION, new FenetreAnticipation(3, 0),
                TypeAnticipation.FIN_CONTRAT, new FenetreAnticipation(3, 1),
                TypeAnticipation.TITULARISATION, new FenetreAnticipation(3, 1)));
        when(moteur.calculerEcheances(agent)).thenReturn(List.of(
                echeance(TypeAnticipation.DEPART_RETRAITE, LocalDate.of(2026, 11, 30)),
                echeance(TypeAnticipation.AVANCEMENT, LocalDate.of(2026, 12, 1)),
                echeance(TypeAnticipation.INTEGRATION, LocalDate.of(2026, 10, 31)),
                echeance(TypeAnticipation.FIN_CONTRAT, LocalDate.of(2027, 1, 1)),
                echeance(TypeAnticipation.TITULARISATION, LocalDate.of(2027, 12, 31))));

        EcheancesAVenirDTO dto = service.echeances(30, AUJOURDHUI, FiltreAgents.AUCUN);

        assertThat(dto.moisProchain().du()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(dto.moisProchain().au()).isEqualTo(LocalDate.of(2026, 11, 30));
        assertThat(dto.moisProchain().total()).isEqualTo(1);
        assertThat(dto.anneeProchaine().du()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(dto.anneeProchaine().au()).isEqualTo(LocalDate.of(2027, 12, 31));
        assertThat(dto.anneeProchaine().total()).isEqualTo(2);
        assertThat(dto.enPreparation().values().stream().mapToLong(Long::longValue).sum()).isEqualTo(4);
        assertThat(dto.enPreparation().get(CategorieCalendrier.TITULARISATION)).isZero();
    }

    @Test
    void moisProchainEnDecembrePasseEnJanvierDeLAnneeSuivante() {
        when(agentRepository.findAllActifs()).thenReturn(List.of());
        EcheancesAVenirDTO dto = service.echeances(30, LocalDate.of(2026, 12, 15), FiltreAgents.AUCUN);
        assertThat(dto.moisProchain().du()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(dto.moisProchain().au()).isEqualTo(LocalDate.of(2027, 1, 31));
        assertThat(dto.anneeProchaine().du()).isEqualTo(LocalDate.of(2027, 1, 1));
    }

    private static Echeance echeance(TypeAnticipation type, LocalDate date) {
        return new Echeance("A", "Agent", type, date, null);
    }
}
