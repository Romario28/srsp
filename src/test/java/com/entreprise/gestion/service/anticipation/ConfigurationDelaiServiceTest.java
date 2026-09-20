package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.ConfigurationDelaiRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Résolution de la fenêtre : base si présente ET active, sinon valeur par défaut codée en dur.
 * C'est cette méthode unique qu'utilisent le batch nocturne et les endpoints API.
 */
class ConfigurationDelaiServiceTest {

    private final ConfigurationDelaiRepository repository = mock(ConfigurationDelaiRepository.class);
    private final ConfigurationDelaiService service = new ConfigurationDelaiService(repository);

    @Test
    @DisplayName("Aucune ligne en base → couple par défaut")
    void aucuneSurcharge() {
        assertEquals(FenetresParDefaut.pour(TypeAnticipation.AVANCEMENT),
                service.resoudreFenetre(TypeAnticipation.AVANCEMENT));
    }

    @Test
    @DisplayName("Ligne active → elle surcharge le défaut (les deux bornes)")
    void surchargeActive() {
        when(repository.findById(TypeAnticipation.AVANCEMENT))
                .thenReturn(Optional.of(ligne(TypeAnticipation.AVANCEMENT, 120, 30, true)));

        assertEquals(new FenetreAnticipation(120, 30), service.resoudreFenetre(TypeAnticipation.AVANCEMENT));
        assertTrue(service.configurationEnBase(TypeAnticipation.AVANCEMENT).isActif());
    }

    @Test
    @DisplayName("Ligne inactive → le défaut reprend la main")
    void surchargeInactive() {
        when(repository.findById(TypeAnticipation.FIN_CONTRAT))
                .thenReturn(Optional.of(ligne(TypeAnticipation.FIN_CONTRAT, 200, 90, false)));

        assertEquals(FenetresParDefaut.pour(TypeAnticipation.FIN_CONTRAT),
                service.resoudreFenetre(TypeAnticipation.FIN_CONTRAT));
    }

    @Test
    @DisplayName("Ligne héritée (retard non renseigné) → prévenance en base, retard par défaut")
    void surchargePartielle() {
        when(repository.findById(TypeAnticipation.TITULARISATION))
                .thenReturn(Optional.of(ligne(TypeAnticipation.TITULARISATION, 200, null, true)));

        assertEquals(new FenetreAnticipation(200, FenetresParDefaut.pour(TypeAnticipation.TITULARISATION).retardJours()),
                service.resoudreFenetre(TypeAnticipation.TITULARISATION));
    }

    @Test
    @DisplayName("resoudreToutes couvre les 4 types configurables, ANOMALIE exclue")
    void resolutionGroupee() {
        when(repository.findById(TypeAnticipation.FIN_CONTRAT))
                .thenReturn(Optional.of(ligne(TypeAnticipation.FIN_CONTRAT, 45, 7, true)));

        Map<TypeAnticipation, FenetreAnticipation> toutes = service.resoudreToutes();

        assertEquals(4, toutes.size());
        assertFalse(toutes.containsKey(TypeAnticipation.ANOMALIE));
        assertEquals(new FenetreAnticipation(45, 7), toutes.get(TypeAnticipation.FIN_CONTRAT));
        assertEquals(FenetresParDefaut.pour(TypeAnticipation.AVANCEMENT), toutes.get(TypeAnticipation.AVANCEMENT));
    }

    @Test
    @DisplayName("definir : les deux bornes sont enregistrées et la surcharge est active")
    void definirCreeLaSurcharge() {
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.empty());
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(inv -> inv.getArgument(0));

        service.definir(TypeAnticipation.AVANCEMENT, 120, 30, null);

        ConfigurationDelai enregistree = capturerSave();
        assertEquals(TypeAnticipation.AVANCEMENT, enregistree.getType());
        assertEquals(120, enregistree.getDelaiPrevenanceJours());
        assertEquals(30, enregistree.getRetardJours());
        assertTrue(enregistree.isActif());
    }

    @Test
    @DisplayName("definir : un retard non transmis conserve la borne de retard effective")
    void definirConserveLeRetard() {
        when(repository.findById(TypeAnticipation.DEPART_RETRAITE))
                .thenReturn(Optional.of(ligne(TypeAnticipation.DEPART_RETRAITE, 300, 45, true)));
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(inv -> inv.getArgument(0));

        service.definir(TypeAnticipation.DEPART_RETRAITE, 400, null, null);

        ConfigurationDelai enregistree = capturerSave();
        assertEquals(400, enregistree.getDelaiPrevenanceJours());
        assertEquals(45, enregistree.getRetardJours());
        assertTrue(enregistree.isActif());
    }

    @Test
    @DisplayName("definir : actif = false neutralise la surcharge sans la supprimer")
    void definirDesactive() {
        when(repository.findById(TypeAnticipation.AVANCEMENT))
                .thenReturn(Optional.of(ligne(TypeAnticipation.AVANCEMENT, 120, 30, true)));
        when(repository.save(any(ConfigurationDelai.class))).thenAnswer(inv -> inv.getArgument(0));

        service.definir(TypeAnticipation.AVANCEMENT, 120, 7, false);

        assertFalse(capturerSave().isActif());
        // La surcharge, bien qu'enregistrée, est ignorée : le défaut reprend la main.
        assertEquals(FenetresParDefaut.pour(TypeAnticipation.AVANCEMENT),
                service.resoudreFenetre(TypeAnticipation.AVANCEMENT));
    }

    @Test
    @DisplayName("definir : bornes négatives et type ANOMALIE refusés")
    void definirValideLesEntrees() {
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class,
                () -> service.definir(TypeAnticipation.AVANCEMENT, -1, 30, null));
        assertThrows(BusinessException.class,
                () -> service.definir(TypeAnticipation.AVANCEMENT, 120, -1, null));
        assertThrows(BusinessException.class,
                () -> service.definir(TypeAnticipation.ANOMALIE, 120, 30, null));

        verify(repository, never()).save(any(ConfigurationDelai.class));
    }

    @Test
    @DisplayName("reinitialiser : la ligne est supprimée, le type retombe sur son défaut")
    void reinitialiser() {
        ConfigurationDelai surcharge = ligne(TypeAnticipation.AVANCEMENT, 120, 30, true);
        when(repository.findById(TypeAnticipation.AVANCEMENT)).thenReturn(Optional.of(surcharge));

        service.reinitialiser(TypeAnticipation.AVANCEMENT);

        verify(repository).delete(surcharge);
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private ConfigurationDelai capturerSave() {
        ArgumentCaptor<ConfigurationDelai> captor = ArgumentCaptor.forClass(ConfigurationDelai.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    private static ConfigurationDelai ligne(TypeAnticipation type, Integer prevenanceJours,
                                            Integer retardJours, boolean actif) {
        return ConfigurationDelai.builder()
                .type(type)
                .delaiPrevenanceJours(prevenanceJours)
                .retardJours(retardJours)
                .actif(actif)
                .build();
    }
}
