package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.service.dossier.DossierResume;
import com.entreprise.gestion.service.dossier.EtatDossier;
import com.entreprise.gestion.service.dossier.SuiviDossiers;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnticipationServiceCritereTest {
    private static final TypeAnticipation TYPE=TypeAnticipation.DEPART_RETRAITE;
    @Mock AgentRepository agentRepository;
    @Mock MoteurAnticipation moteur;
    @Mock ConfigurationDelaiService configurationDelaiService;
    @Mock GradeSuivantService gradeSuivantService;
    @Mock SuiviDossiers suiviDossiers;
    @InjectMocks AnticipationService service;

    private final LocalDate aujourdhui=LocalDate.now();
    private final LocalDate echeanceFutur=aujourdhui.plusMonths(9).plusDays(10);
    private final LocalDate echeancePasse=aujourdhui.plusMonths(2).plusDays(20);
    private final LocalDate echeanceCourant=aujourdhui.plusMonths(5).plusDays(5);

    @BeforeEach void preparer() {
        Agent futur=agent("FUTUR"), passe=agent("PASSE"), courant=agent("COURANT");
        when(configurationDelaiService.resoudreFenetre(TYPE)).thenReturn(new FenetreAnticipation(6,2));
        when(suiviDossiers.tous()).thenReturn(Map.of());
        when(agentRepository.findAllActifs()).thenReturn(List.of(futur,passe,courant));
        when(moteur.calculerEcheances(futur)).thenReturn(List.of(echeance(futur,echeanceFutur)));
        when(moteur.calculerEcheances(passe)).thenReturn(List.of(echeance(passe,echeancePasse)));
        when(moteur.calculerEcheances(courant)).thenReturn(List.of(echeance(courant,echeanceCourant)));
    }
    private static Agent agent(String matricule) {
        return Agent.builder().matricule(matricule).prenoms("Jean").nom(matricule).statut(StatutAgent.FONCTIONNAIRE).build();
    }
    private static Echeance echeance(Agent a,LocalDate date) {
        return new Echeance(a.getMatricule(),a.getPrenoms()+" "+a.getNom(),TYPE,date,null);
    }
    private List<String> matricules(CritereDate critere,Integer prevenance,Integer retard,LocalDate debut,LocalDate fin) {
        return service.departsRetraite(prevenance,retard,debut,fin,critere,FiltreAgents.AUCUN)
                .stream().map(EcheanceAnticipeeDTO::matricule).toList();
    }
    @Test void fenetreRelativeEcheanceEstInchangee() {
        assertThat(matricules(CritereDate.ECHEANCE,null,null,null,null)).containsExactlyInAnyOrder("PASSE","COURANT");
        assertThat(matricules(null,null,null,null,null)).containsExactlyInAnyOrder("PASSE","COURANT");
    }
    @Test void fenetreRelativePreparationSePoseAutourDeLaDateDePreparation() {
        assertThat(matricules(CritereDate.PREPARATION,null,null,null,null)).containsExactlyInAnyOrder("FUTUR","COURANT");
    }
    @Test void fenetreSaisieNeDecalePasLaDatePreparation() {
        List<EcheanceAnticipeeDTO> lignes=service.departsRetraite(12,null,null,null,CritereDate.PREPARATION,FiltreAgents.AUCUN);
        assertThat(lignes).extracting(EcheanceAnticipeeDTO::matricule).containsExactlyInAnyOrder("FUTUR","COURANT");
        assertThat(lignes).allSatisfy(l -> assertThat(l.datePreparation()).isEqualTo(l.dateEcheance().minusMonths(6)));
    }
    @Test void intervalleDeDatesPorteSurLaDateDuCritere() {
        LocalDate prepFutur=echeanceFutur.minusMonths(6);
        LocalDate debut=prepFutur.minusDays(2),fin=prepFutur.plusDays(2);
        assertThat(matricules(CritereDate.PREPARATION,null,null,debut,fin)).containsExactly("FUTUR");
        assertThat(matricules(CritereDate.ECHEANCE,null,null,debut,fin)).isEmpty();
    }

    @Test void ligneExposeEtatEtResumeDuDossierLie() {
        DossierResume lie=new DossierResume("COURANT",TYPE,echeanceCourant,StatutDossier.EN_COURS,
                aujourdhui.minusDays(3),null);
        when(suiviDossiers.tous()).thenReturn(Map.of(lie.cle(),lie));
        List<AnticipationService.AgentEcheance> lignes=service.echeancesAvecAgent(TYPE,null,null,null,null,
                CritereDate.ECHEANCE,FiltreAgents.AUCUN);
        assertThat(lignes).filteredOn(ae->ae.ligne().matricule().equals("COURANT")).singleElement().satisfies(ae->{
            assertThat(ae.ligne().dossier()).isEqualTo(EtatDossier.EN_COURS);
            assertThat(ae.dossier()).isSameAs(lie);
        });
        assertThat(lignes).filteredOn(ae->!ae.ligne().matricule().equals("COURANT"))
                .allSatisfy(ae->assertThat(ae.ligne().dossier()).isNull());
    }
}
