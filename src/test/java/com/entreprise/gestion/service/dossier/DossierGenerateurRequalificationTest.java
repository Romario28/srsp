package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.config.anticipation.DossierProperties;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.anticipation.DossierRepository;
import com.entreprise.gestion.service.AuditService;
import com.entreprise.gestion.service.anticipation.Echeance;
import com.entreprise.gestion.service.anticipation.FenetreAnticipation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierGenerateurRequalificationTest {
    private static final LocalDate AUJ = LocalDate.of(2026, 10, 16);
    private static final LocalDate ECHEANCE = AUJ.plusDays(10);
    @Mock DossierRepository dossierRepository;
    @Mock DossierProperties proprietes;
    @Mock EntityManager entityManager;
    @Mock AuditService auditService;
    @InjectMocks DossierGenerateur generateur;

    private final Agent agent = Agent.builder().matricule("123456").build();
    private final Echeance classe = new Echeance("123456", "Jean Rakoto", TypeAnticipation.AVANCEMENT_CLASSE, ECHEANCE, null);

    @BeforeEach void preparer() {
        lenient().when(proprietes.getDateReprise()).thenReturn(LocalDate.of(2026, 1, 1));
        lenient().when(proprietes.getTypesSuivis()).thenReturn(EnumSet.allOf(TypeAnticipation.class));
    }

    @Test void unDossierDejaOuvertSousAvancementEstRequalifieEtNonDouble() {
        Dossier ancien = Dossier.builder().id(1L).agent(agent).type(TypeAnticipation.AVANCEMENT)
                .datePreparation(ECHEANCE.minusMonths(6)).dateEcheance(ECHEANCE).statut(StatutDossier.EN_COURS).build();
        when(dossierRepository.cles()).thenReturn(List.of(new CleDossier("123456", TypeAnticipation.AVANCEMENT, ECHEANCE)));
        when(dossierRepository.findByAgentMatriculeAndTypeAndDateEcheance("123456", TypeAnticipation.AVANCEMENT, ECHEANCE))
                .thenReturn(Optional.of(ancien));

        DossierGenerateur.Passage passage = generateur.ouvrir(AUJ);
        passage.traiter(agent, classe, new FenetreAnticipation(1, 0));

        assertThat(ancien.getType()).isEqualTo(TypeAnticipation.AVANCEMENT_CLASSE);
        assertThat(ancien.getStatut()).isEqualTo(StatutDossier.EN_COURS);   // l'état du dossier est conservé
        verify(dossierRepository, times(1)).save(any());
        assertThat(passage.terminer()).isZero();
    }

    @Test void sansDossierPreexistantUnDossierEstCreeSousLeNouveauType() {
        when(dossierRepository.cles()).thenReturn(List.of());

        DossierGenerateur.Passage passage = generateur.ouvrir(AUJ);
        passage.traiter(agent, classe, new FenetreAnticipation(1, 0));   // préparation = échéance − 1 mois ≤ aujourd'hui

        verify(dossierRepository).save(argThat(d -> d.getType() == TypeAnticipation.AVANCEMENT_CLASSE));
        assertThat(passage.terminer()).isEqualTo(1);
    }
}
