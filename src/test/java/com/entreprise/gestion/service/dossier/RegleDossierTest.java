package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegleDossierTest {
    private final LocalDate auj=LocalDate.of(2026,10,16);
    private static Dossier dossier(LocalDate preparation){return Dossier.builder().type(TypeAnticipation.AVANCEMENT).datePreparation(preparation).dateEcheance(preparation.plusMonths(3)).build();}
    private void refuse(Dossier d,String code){assertThatThrownBy(()->RegleDossier.verifier(d,auj)).isInstanceOfSatisfying(BusinessException.class,e->assertThat(e.getCode()).isEqualTo(code));}
    @Test void statutSuitLesEtapes(){
        Dossier d=dossier(auj.minusDays(5));assertThat(RegleDossier.statutSelonEtapes(d)).isEqualTo(StatutDossier.EN_ATTENTE_DE_TRAITEMENT);
        d.setDateChefAvantFop(auj.minusDays(2));assertThat(RegleDossier.statutSelonEtapes(d)).isEqualTo(StatutDossier.EN_COURS);
        d.setDateFin(auj.minusDays(1));assertThat(RegleDossier.statutSelonEtapes(d)).isEqualTo(StatutDossier.TRAITE);
    }
    @Test void enRetardSiNonDemarreEtPreparationDepassee(){assertThat(RegleDossier.enRetard(dossier(auj.minusDays(1)),auj)).isTrue();assertThat(RegleDossier.enRetard(dossier(auj),auj)).isFalse();}
    @Test void unDossierEnCoursNEstJamaisEnRetard(){Dossier d=dossier(auj.minusMonths(6));d.setDateChefAvantFop(auj.minusMonths(5));d.setStatut(StatutDossier.EN_COURS);assertThat(RegleDossier.enRetard(d,auj)).isFalse();}
    @Test void aRelancerSiEnCoursSansNouvelleDepuisPlusDeNJours(){
        Dossier d=dossier(auj.minusMonths(3));d.setDateChefAvantFop(auj.minusDays(40));d.setStatut(StatutDossier.EN_COURS);
        assertThat(RegleDossier.aRelancer(d,auj,30)).isTrue();d.setDateFop(auj.minusDays(10));assertThat(RegleDossier.aRelancer(d,auj,30)).isFalse();
        assertThat(RegleDossier.aRelancer(dossier(auj.minusMonths(3)),auj,30)).isFalse();
    }
    @Test void uneEtapeSansDepartEstRefusee(){Dossier d=dossier(auj.minusDays(5));d.setDateVisa(auj.minusDays(1));refuse(d,"ETAPE_SANS_DEPART");}
    @Test void uneDateFutureEstRefusee(){Dossier d=dossier(auj.minusDays(5));d.setDateChefAvantFop(auj.plusDays(1));refuse(d,"DATE_FUTURE");}
    @Test void lOrdreDeLaChaineEstImpose(){
        Dossier d=dossier(auj.minusDays(30));d.setDateChefAvantFop(auj.minusDays(20));d.setDateFop(auj.minusDays(10));d.setDateVisa(auj.minusDays(15));refuse(d,"ORDRE_ETAPES");
    }
    @Test void lesEtapesNonApplicablesPeuventEtreSautees(){
        Dossier d=dossier(auj.minusDays(30));d.setDateChefAvantFop(auj.minusDays(20));d.setDateCf(auj.minusDays(10));d.setDateFin(auj.minusDays(10));
        assertThatCode(()->RegleDossier.verifier(d,auj)).doesNotThrowAnyException();
    }
}
