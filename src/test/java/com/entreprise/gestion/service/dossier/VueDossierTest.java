package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.StatutDossier;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class VueDossierTest {
    private final LocalDate auj=LocalDate.of(2026,10,16);
    private static List<String> ordre(Sort tri){return tri.stream().map(o->o.getProperty()+" "+o.getDirection()).toList();}
    @Test void aTraiterPeriodeOuverteAujourdhuiCompris(){
        VueDossier.Bornes b=VueDossier.A_TRAITER.bornes(auj);
        assertThat(b.preparationMax()).isEqualTo(auj);assertThat(b.echeanceMin()).isEqualTo(auj);assertThat(b.echeanceMax()).isNull();
    }
    @Test void enRetardEcheanceStrictementDepassee(){
        VueDossier.Bornes b=VueDossier.EN_RETARD.bornes(auj);
        assertThat(b.echeanceMax()).isEqualTo(auj.minusDays(1));assertThat(b.preparationMax()).isNull();assertThat(b.echeanceMin()).isNull();
    }
    @Test void aTraiterEtEnRetardNeSeRecouvrentJamais(){assertThat(VueDossier.EN_RETARD.bornes(auj).echeanceMax()).isBefore(VueDossier.A_TRAITER.bornes(auj).echeanceMin());}
    @Test void lesAutresVuesNOntPasDeBornes(){
        for(VueDossier v:new VueDossier[]{VueDossier.EN_COURS,VueDossier.SANS_SUITE,VueDossier.TOUS})
            assertThat(v.bornes(auj)).isEqualTo(new VueDossier.Bornes(null,null,null));
    }
    @Test void tousCouvreLesTroisStatuts(){
        assertThat(VueDossier.TOUS.statuts()).containsExactlyInAnyOrder(
                StatutDossier.EN_ATTENTE_DE_TRAITEMENT, StatutDossier.EN_COURS, StatutDossier.SANS_SUITE);
    }
    @Test void aTraiterParDefautEcheanceProcheDAbord(){assertThat(ordre(VueDossier.A_TRAITER.tri(false))).containsExactly("dateEcheance ASC","datePreparation ASC","id ASC");}
    @Test void aTraiterInversePreparationRecenteDAbord(){assertThat(ordre(VueDossier.A_TRAITER.tri(true))).containsExactly("datePreparation DESC","dateEcheance ASC","id ASC");}
    @Test void enRetardParDefautPlusAnciensDAbordEtInversePlusRecentsDAbord(){
        assertThat(ordre(VueDossier.EN_RETARD.tri(false))).containsExactly("dateEcheance ASC","id ASC");
        assertThat(ordre(VueDossier.EN_RETARD.tri(true))).containsExactly("dateEcheance DESC","id ASC");
    }
    @Test void lesAutresVuesIgnorentLeTriInverse(){
        for(VueDossier v:new VueDossier[]{VueDossier.EN_COURS,VueDossier.SANS_SUITE,VueDossier.TOUS})
            assertThat(v.tri(true)).as(v.name()).isEqualTo(v.tri(false));
    }
}
