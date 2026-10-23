package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.config.anticipation.DossierProperties;
import com.entreprise.gestion.dto.dossier.DossierDTO;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.DossierRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.AuditService;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DossierServiceTest {
    @Mock DossierRepository dossierRepository;
    @Mock DossierProperties proprietes;
    @Mock AuditService auditService;
    @InjectMocks DossierService service;

    private final UserDetailsImpl acteur = new UserDetailsImpl(Utilisateur.builder().id(1L).email("admin@entreprise.mg").build());

    private Dossier trouve(StatutDossier statut, LocalDate echeance, LocalDate demarrage) {
        Agent agent = Agent.builder().matricule("123456").prenoms("Jean").nom("Rakoto").build();
        Dossier d = Dossier.builder().id(7L).agent(agent).type(TypeAnticipation.AVANCEMENT)
                .datePreparation(echeance.minusMonths(3)).dateEcheance(echeance).statut(statut).dateDemarrage(demarrage).build();
        when(dossierRepository.findById(7L)).thenReturn(Optional.of(d));
        return d;
    }
    private void refuse(ThrowingCallable action, String code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }

    @Test void demarrerPasseEnCoursEtPoseLaDateDuJour() {
        Dossier d = trouve(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, LocalDate.now().plusMonths(1), null);
        DossierDTO dto = service.demarrer(7L, acteur);
        assertThat(d.getStatut()).isEqualTo(StatutDossier.EN_COURS);
        assertThat(d.getDateDemarrage()).isEqualTo(LocalDate.now());
        assertThat(dto.statut()).isEqualTo(StatutDossier.EN_COURS);
        assertThat(dto.enRetard()).isFalse();
        verify(dossierRepository).saveAndFlush(d);
        verify(auditService).logAvecEntite(any(), eq("DOSSIER_DEMARRE"), any(), isNull());
    }
    @Test void demarrerUnDossierEnRetardEstPermisEtTrace() {
        Dossier d = trouve(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, LocalDate.now().minusDays(5), null);
        service.demarrer(7L, acteur);
        assertThat(d.getStatut()).isEqualTo(StatutDossier.EN_COURS);
        verify(auditService).logAvecEntite(any(), eq("DOSSIER_DEMARRE"), contains("en retard"), isNull());
    }
    @Test void demarrerRefuseUnDossierDejaEnCours() {
        trouve(StatutDossier.EN_COURS, LocalDate.now().plusDays(10), LocalDate.now().minusDays(1));
        refuse(() -> service.demarrer(7L, acteur), "DEJA_DEMARRE");
        verify(dossierRepository, never()).saveAndFlush(any());
    }
    @Test void demarrerRefuseUnDossierSansSuite() {
        trouve(StatutDossier.SANS_SUITE, LocalDate.now().plusDays(10), null);
        refuse(() -> service.demarrer(7L, acteur), "DOSSIER_SANS_SUITE");
        verify(dossierRepository, never()).saveAndFlush(any());
    }

    @Test void sansSuiteExigeUnMotif() {
        trouve(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, LocalDate.now().plusDays(10), null);
        refuse(() -> service.classerSansSuite(7L, "   ", acteur), "MOTIF_REQUIS");
        verify(dossierRepository, never()).saveAndFlush(any());
    }
    @Test void sansSuiteDepuisEnCoursConserveLaDateDeDemarrage() {
        LocalDate demarre = LocalDate.now().minusDays(4);
        Dossier d = trouve(StatutDossier.EN_COURS, LocalDate.now().plusDays(10), demarre);
        service.classerSansSuite(7L, "  Agent démissionnaire ", acteur);
        assertThat(d.getStatut()).isEqualTo(StatutDossier.SANS_SUITE);
        assertThat(d.getMotifSansSuite()).isEqualTo("Agent démissionnaire");
        assertThat(d.getDateDemarrage()).isEqualTo(demarre);
    }
    @Test void sansSuiteRefuseUnDossierDejaClos() {
        trouve(StatutDossier.SANS_SUITE, LocalDate.now().plusDays(10), null);
        refuse(() -> service.classerSansSuite(7L, "motif", acteur), "DOSSIER_CLOS");
    }

    @Test void reouvrirRetrouveEnCoursSiLeDossierAvaitEteDemarre() {
        Dossier d = trouve(StatutDossier.SANS_SUITE, LocalDate.now().plusDays(10), LocalDate.now().minusDays(2));
        d.setMotifSansSuite("abandon");
        service.reouvrir(7L, acteur);
        assertThat(d.getStatut()).isEqualTo(StatutDossier.EN_COURS);
        assertThat(d.getMotifSansSuite()).isNull();
    }
    @Test void reouvrirRetrouveATraiterSiLeDossierNAvaitJamaisEteDemarre() {
        Dossier d = trouve(StatutDossier.SANS_SUITE, LocalDate.now().plusDays(10), null);
        service.reouvrir(7L, acteur);
        assertThat(d.getStatut()).isEqualTo(StatutDossier.EN_ATTENTE_DE_TRAITEMENT);
    }
    @Test void reouvrirRefuseUnDossierNonClasse() {
        trouve(StatutDossier.EN_COURS, LocalDate.now().plusDays(10), LocalDate.now());
        refuse(() -> service.reouvrir(7L, acteur), "DOSSIER_NON_CLASSE");
    }
}
