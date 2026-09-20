package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.ConfigurationDelaiRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Résolution « config en base si présente et active, sinon défaut ». */
class ConfigurationDelaiServiceTest {

    private ConfigurationDelaiRepository repository;
    private ConfigurationDelaiService service;

    @BeforeEach
    void setUp() {
        repository = mock(ConfigurationDelaiRepository.class);
        service = new ConfigurationDelaiService(repository);
    }

    @Test
    void sansLigneEnBase_onRetombeSurLeDefaut() {
        when(repository.findById(TypeAnticipation.DEPART_RETRAITE)).thenReturn(Optional.empty());

        assertThat(service.resoudrePlage(TypeAnticipation.DEPART_RETRAITE))
                .isEqualTo(FenetresParDefaut.pour(TypeAnticipation.DEPART_RETRAITE));
        assertThat(service.estPersonnalise(TypeAnticipation.DEPART_RETRAITE)).isFalse();
    }

    @Test
    void ligneInactive_estIgnoree() {
        ConfigurationDelai desactivee = ConfigurationDelai.builder()
                .type(TypeAnticipation.AVANCEMENT)
                .delaiPrevenanceJours(10)
                .retardJours(5)
                .actif(false)
                .build();
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.of(desactivee));

        assertThat(service.resoudrePlage(TypeAnticipation.AVANCEMENT))
                .isEqualTo(FenetresParDefaut.pour(TypeAnticipation.AVANCEMENT));
        assertThat(service.estPersonnalise(TypeAnticipation.AVANCEMENT)).isFalse();
    }

    @Test
    void ligneActive_surchargeLesDeuxBornes() {
        ConfigurationDelai surcharge = ConfigurationDelai.builder()
                .type(TypeAnticipation.FIN_CONTRAT)
                .delaiPrevenanceJours(120)
                .retardJours(30)
                .actif(true)
                .build();
        when(repository.findById(TypeAnticipation.FIN_CONTRAT)).thenReturn(Optional.of(surcharge));

        assertThat(service.resoudrePlage(TypeAnticipation.FIN_CONTRAT))
                .isEqualTo(new FenetreAnticipation(120, 30));
        assertThat(service.estPersonnalise(TypeAnticipation.FIN_CONTRAT)).isTrue();
        assertThat(service.resoudreDelai(TypeAnticipation.FIN_CONTRAT)).isEqualTo(120);
    }

    @Test
    void ligneHeriteeSansRetard_retombeSurLeRetardParDefautDuType() {
        ConfigurationDelai heritee = ConfigurationDelai.builder()
                .type(TypeAnticipation.DEPART_RETRAITE)
                .delaiPrevenanceJours(400)
                .retardJours(null)
                .actif(true)
                .build();
        when(repository.findById(TypeAnticipation.DEPART_RETRAITE)).thenReturn(Optional.of(heritee));

        FenetreAnticipation attendue = new FenetreAnticipation(
                400, FenetresParDefaut.pour(TypeAnticipation.DEPART_RETRAITE).retardJours());

        assertThat(service.resoudrePlage(TypeAnticipation.DEPART_RETRAITE)).isEqualTo(attendue);
    }

    @Test
    void borneNegativeSaisieEnBase_estRameneAZero() {
        ConfigurationDelai incoherente = ConfigurationDelai.builder()
                .type(TypeAnticipation.TITULARISATION)
                .delaiPrevenanceJours(-5)
                .retardJours(-2)
                .actif(true)
                .build();
        when(repository.findById(TypeAnticipation.TITULARISATION)).thenReturn(Optional.of(incoherente));

        assertThat(service.resoudrePlage(TypeAnticipation.TITULARISATION))
                .isEqualTo(new FenetreAnticipation(0, 0));
    }

    @Test
    void definirEnregistreLesDeuxBornesEtActiveLaSurcharge() {
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.empty());
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationDelai enregistree = service.definir(TypeAnticipation.AVANCEMENT, 120, 30, null);

        assertThat(enregistree.getType()).isEqualTo(TypeAnticipation.AVANCEMENT);
        assertThat(enregistree.getDelaiPrevenanceJours()).isEqualTo(120);
        assertThat(enregistree.getRetardJours()).isEqualTo(30);
        assertThat(enregistree.isActif()).isTrue();
    }

    @Test
    void definirSansRetard_conserveLeRetardDejaEnregistre() {
        ConfigurationDelai existante = ConfigurationDelai.builder()
                .type(TypeAnticipation.AVANCEMENT)
                .delaiPrevenanceJours(90)
                .retardJours(45)
                .actif(true)
                .build();
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.of(existante));
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationDelai enregistree = service.definir(TypeAnticipation.AVANCEMENT, 60, null, null);

        assertThat(enregistree.getDelaiPrevenanceJours()).isEqualTo(60);
        assertThat(enregistree.getRetardJours()).isEqualTo(45);
    }

    @Test
    void definirSansPrevenance_conserveLaPrevenanceDejaEnregistree() {
        ConfigurationDelai existante = ConfigurationDelai.builder()
                .type(TypeAnticipation.AVANCEMENT)
                .delaiPrevenanceJours(75)
                .retardJours(45)
                .actif(true)
                .build();
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.of(existante));
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationDelai enregistree = service.definir(TypeAnticipation.AVANCEMENT, null, 0, null);

        assertThat(enregistree.getDelaiPrevenanceJours()).isEqualTo(75);
        assertThat(enregistree.getRetardJours()).isZero();
    }

    @Test
    void definirSansAucuneSurchargeExistante_conserveLeDefautDuType() {
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.empty());
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationDelai enregistree = service.definir(TypeAnticipation.AVANCEMENT, null, 15, null);

        FenetreAnticipation defaut = FenetresParDefaut.pour(TypeAnticipation.AVANCEMENT);
        assertThat(enregistree.getDelaiPrevenanceJours()).isEqualTo(defaut.prevenanceJours());
        assertThat(enregistree.getRetardJours()).isEqualTo(15);
    }

    @Test
    void definirPeutDesactiverLaSurcharge() {
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.empty());
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConfigurationDelai enregistree = service.definir(TypeAnticipation.AVANCEMENT, 120, 30, false);

        assertThat(enregistree.isActif()).isFalse();
    }

    @Test
    void bornesNegatives_refusees() {
        assertThatThrownBy(() -> service.definir(TypeAnticipation.AVANCEMENT, -1, 0, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "PREVENANCE_INVALIDE");
        assertThatThrownBy(() -> service.definir(TypeAnticipation.AVANCEMENT, 0, -1, null))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("code", "RETARD_INVALIDE");
    }

    @Test
    void anomalie_nEstPasConfigurable() {
        assertThatThrownBy(() -> service.definir(TypeAnticipation.ANOMALIE, 10, 0, null))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> service.reinitialiser(TypeAnticipation.ANOMALIE))
                .isInstanceOf(BusinessException.class);
        verify(repository, never()).save(any(ConfigurationDelai.class));
    }

    @Test
    void reinitialiser_supprimeLaSurcharge() {
        ConfigurationDelai surcharge = ConfigurationDelai.builder()
                .type(TypeAnticipation.FIN_CONTRAT)
                .delaiPrevenanceJours(120)
                .retardJours(30)
                .actif(true)
                .build();
        when(repository.findById(TypeAnticipation.FIN_CONTRAT)).thenReturn(Optional.of(surcharge));

        service.reinitialiser(TypeAnticipation.FIN_CONTRAT);

        verify(repository).delete(surcharge);
    }

    @Test
    void resoudreToutes_combineSurchargesActivesEtDefauts() {
        ConfigurationDelai surcharge = ConfigurationDelai.builder()
                .type(TypeAnticipation.FIN_CONTRAT)
                .delaiPrevenanceJours(120)
                .retardJours(15)
                .actif(true)
                .build();
        when(repository.findByActifTrue()).thenReturn(List.of(surcharge));

        Map<TypeAnticipation, FenetreAnticipation> fenetres = service.resoudreToutes();

        assertThat(fenetres)
                .containsOnlyKeys(FenetresParDefaut.typesConfigurables().toArray(new TypeAnticipation[0]));
        assertThat(fenetres.get(TypeAnticipation.FIN_CONTRAT)).isEqualTo(new FenetreAnticipation(120, 15));
        assertThat(fenetres.get(TypeAnticipation.DEPART_RETRAITE))
                .isEqualTo(FenetresParDefaut.pour(TypeAnticipation.DEPART_RETRAITE));
    }
}
