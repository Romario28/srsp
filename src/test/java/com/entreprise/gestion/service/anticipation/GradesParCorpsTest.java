package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.CasGradeSuivant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GradesParCorpsTest {

    private static LigneIndice l(String corps, String cat, String grade, String indice) {
        return new LigneIndice(corps, cat, grade, indice);
    }

    private static GradeSuivant unique(String code) {
        return new GradeSuivant(CasGradeSuivant.UNIQUE, List.of(code));
    }

    private final RenommagesGrades renommages = new RenommagesGrades(
            Map.of("MC1E", "ST0E", "MC2E", "2C1E", "MC3E", "2C2E", "EX0E", "EX1E"),
            Map.of("A88L", Map.of("1C4E", "PR1E"), "J88L", Map.of("1C4E", "PR1E"),
                    "U88L", Map.of("1C4E", "PR1E")));

    private final GradesParCorps grades = new GradesParCorps(List.of(
            l("A88J", "10", "MC1E", "3 850"), l("A88J", "10", "ST0E", "3 850"),
            l("A88J", "10", "MC2E", "4 250"), l("A88J", "10", "2C1E", "4 250"),
            l("A88J", "10", "2C2E", "4 350"), l("A88J", "10", "MC3E", "4 350"),
            l("A88J", "10", "1C3E", "4 800"), l("A88J", "10", "PR1E", "4 950"),
            l("A88J", "10", "PR3E", "5 100"), l("A88J", "10", "EX0E", "5 700"),
            l("A88J", "10", "EX1E", "5 700"), l("A88J", "10", "EX2E", "5 800"),
            l("A88L", "10", "1C3E", "4 800"), l("A88L", "10", "1C4E", "4 950"),
            l("A88L", "10", "PR1E", "4 950"), l("A88L", "10", "PR2E", "5 050"),
            l("A00X", "10", "1C3E", "4 800"), l("A00X", "10", "1C4E", "4 950"),
            l("A00X", "10", "PR1E", "4 950"),
            l("A15A", "09", "G23E", "3 900"), l("A15A", "09", "G11E", "4 010"),
            l("A15A", "09", "G12E", "4 010"),
            l("B77B", "10", "MC2E", "4 250"), l("B77B", "10", "2C1E", "4 300"),
            l("B77B", "10", "2C2E", "4 400")
    ), Set.of("ST0E"), renommages);

    @Test void stagiaireVersPremierGradeTitulaire() {
        assertThat(grades.pour("A88J", "10", "ST0E")).isEqualTo(unique("2C1E"));
    }

    @Test void agentResteSousLAncienNomDuStagiaire() {
        assertThat(grades.pour("A88J", "10", "MC1E")).isEqualTo(unique("2C1E"));
    }

    @Test void renommageGlobalAbsorbeLeFauxExAequo() {
        assertThat(grades.pour("A88J", "10", "2C1E")).isEqualTo(unique("2C2E"));
        assertThat(grades.pour("A88J", "10", "PR3E")).isEqualTo(unique("EX1E"));
    }

    @Test void agentResteSousLAncienNom() {
        assertThat(grades.pour("A88J", "10", "MC2E")).isEqualTo(unique("2C2E"));
    }

    @Test void renommageParCorpsAppliqueDansUnCorpsListe() {
        assertThat(grades.pour("A88L", "10", "1C3E")).isEqualTo(unique("PR1E"));
        assertThat(grades.pour("A88L", "10", "1C4E")).isEqualTo(unique("PR2E"));
    }

    @Test void renommageParCorpsNonAppliqueAilleurs() {
        assertThat(grades.pour("A00X", "10", "1C3E"))
                .isEqualTo(new GradeSuivant(CasGradeSuivant.AMBIGU, List.of("1C4E", "PR1E")));
    }

    @Test void dernierGrade() {
        assertThat(grades.pour("A88J", "10", "EX2E")).isEqualTo(GradeSuivant.DERNIER_GRADE);
    }

    @Test void vraiExAequoResteAmbigu() {
        assertThat(grades.pour("A15A", "09", "G23E"))
                .isEqualTo(new GradeSuivant(CasGradeSuivant.AMBIGU, List.of("G11E", "G12E")));
    }

    @Test void indicesDiscordantsSurUnMemeGradeDonnentIndetermine() {
        assertThat(grades.pour("B77B", "10", "2C1E")).isEqualTo(GradeSuivant.INDETERMINE);
    }
}
