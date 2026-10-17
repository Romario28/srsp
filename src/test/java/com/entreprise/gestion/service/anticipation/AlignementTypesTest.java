package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AlignementTypesTest {
    @Test void chaqueTypeAEcheanceADesDefautsEtUneCategorieDeCalendrier() {
        for(TypeAnticipation type:TypeAnticipation.values()) {
            if(type==TypeAnticipation.ANOMALIE) { assertThat(CategorieCalendrier.de(type,null)).isNull(); continue; }
            assertThat(DelaisParDefaut.pour(type)).as("fenêtre par défaut de %s",type).isNotNull();
            assertThat(CategorieCalendrier.de(type,StatutAgent.CONTRACTUEL)).as("catégorie de %s",type).isNotNull();
        }
    }
    @Test void lintegrationADeSaPropreCategorie() {
        assertThat(CategorieCalendrier.de(TypeAnticipation.INTEGRATION,StatutAgent.CONTRACTUEL)).isEqualTo(CategorieCalendrier.INTEGRATION);
    }
}
