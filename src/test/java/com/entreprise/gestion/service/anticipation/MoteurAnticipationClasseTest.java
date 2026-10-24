package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Corps;
import com.entreprise.gestion.entite.anticipation.Grade;
import com.entreprise.gestion.entite.anticipation.IndiceGrdCorps;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class MoteurAnticipationClasseTest {
    @Mock IndiceGrdCorpsRepository indices;
    @Mock GradeSuivantService gradeSuivantService;
    @InjectMocks MoteurAnticipation moteur;

    @BeforeEach void preparer() {
        ReflectionTestUtils.setField(moteur, "gradesStagiaire", Set.of("ST0E"));
        ReflectionTestUtils.setField(moteur, "ageRetraite", 60);
        GradesParCorps index = new GradesParCorps(List.of(
                new LigneIndice("A88J", "10", "ST0E", "3 850"), new LigneIndice("A88J", "10", "2C1E", "4 250"),
                new LigneIndice("A88J", "10", "2C2E", "4 350"), new LigneIndice("A88J", "10", "1C3E", "4 800")),
                Set.of("ST0E"), RenommagesGrades.AUCUN);
        lenient().when(gradeSuivantService.indexer()).thenReturn(index);
        lenient().when(indices.findById(any())).thenReturn(Optional.of(IndiceGrdCorps.builder().dureeRequise(2).build()));
    }

    private TypeAnticipation typeAvancement(String grade) {
        Agent agent = Agent.builder().matricule("123456").prenoms("Jean").nom("Rakoto").statut(StatutAgent.FONCTIONNAIRE)
                .dateNaissance(LocalDate.of(1980, 1, 1)).avanceDate(LocalDate.of(2024, 1, 10))
                .corps(Corps.builder().code("A88J").categorie("10").build())
                .grade(Grade.builder().code(grade).build()).build();
        return moteur.calculerEcheances(agent).stream()
                .filter(e -> e.type() != TypeAnticipation.DEPART_RETRAITE)
                .map(Echeance::type).findFirst().orElseThrow();
    }

    @Test void changementDEchelonResteUnAvancement() { assertThat(typeAvancement("2C1E")).isEqualTo(TypeAnticipation.AVANCEMENT); }
    @Test void changementDesDeuxPremiersCaracteresEstUnAvancementDeClasse() { assertThat(typeAvancement("2C2E")).isEqualTo(TypeAnticipation.AVANCEMENT_CLASSE); }
    @Test void leStagiaireResteUneTitularisationMemeSiLaClasseChange() { assertThat(typeAvancement("ST0E")).isEqualTo(TypeAnticipation.TITULARISATION); }
    @Test void dernierGradeResteUnAvancement() { assertThat(typeAvancement("1C3E")).isEqualTo(TypeAnticipation.AVANCEMENT); }
}
