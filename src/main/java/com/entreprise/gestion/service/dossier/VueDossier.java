package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.StatutDossier;
import java.util.List;
import static com.entreprise.gestion.entite.anticipation.StatutDossier.*;

public enum VueDossier {
    A_TRAITER(EN_ATTENTE_DE_TRAITEMENT,EN_COURS), EN_RETARD(EN_ATTENTE_DE_TRAITEMENT),
    A_RELANCER(EN_COURS), TERMINES(TRAITE,SANS_SUITE), TOUS(EN_ATTENTE_DE_TRAITEMENT,EN_COURS,TRAITE,SANS_SUITE);
    private final List<StatutDossier> statuts;
    VueDossier(StatutDossier... statuts){this.statuts=List.of(statuts);}
    public List<StatutDossier> statuts(){return statuts;}
}
