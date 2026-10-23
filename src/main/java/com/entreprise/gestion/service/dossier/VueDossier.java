package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.StatutDossier;
import org.springframework.data.domain.Sort;
import java.time.LocalDate;
import java.util.List;
import static org.springframework.data.domain.Sort.Order.asc;
import static org.springframework.data.domain.Sort.Order.desc;

/** Vues (onglets) de l'écran, avec bornes et tri centralisés. */
public enum VueDossier {
    A_TRAITER(StatutDossier.EN_ATTENTE_DE_TRAITEMENT),
    EN_RETARD(StatutDossier.EN_ATTENTE_DE_TRAITEMENT),
    EN_COURS(StatutDossier.EN_COURS),
    SANS_SUITE(StatutDossier.SANS_SUITE),
    TOUS(StatutDossier.EN_ATTENTE_DE_TRAITEMENT, StatutDossier.EN_COURS, StatutDossier.SANS_SUITE);

    public record Bornes(LocalDate preparationMax, LocalDate echeanceMin, LocalDate echeanceMax) {}

    private final List<StatutDossier> statuts;
    VueDossier(StatutDossier... statuts) { this.statuts = List.of(statuts); }
    public List<StatutDossier> statuts() { return statuts; }

    public Bornes bornes(LocalDate aujourd) {
        return switch (this) {
            case A_TRAITER -> new Bornes(aujourd, aujourd, null);
            case EN_RETARD -> new Bornes(null, null, aujourd.minusDays(1));
            case EN_COURS, SANS_SUITE, TOUS -> new Bornes(null, null, null);
        };
    }

    public Sort tri(boolean inverse) {
        return switch (this) {
            case A_TRAITER -> inverse ? Sort.by(desc("datePreparation"), asc("dateEcheance"), asc("id"))
                    : Sort.by(asc("dateEcheance"), asc("datePreparation"), asc("id"));
            case EN_RETARD -> inverse ? Sort.by(desc("dateEcheance"), asc("id")) : Sort.by(asc("dateEcheance"), asc("id"));
            case EN_COURS -> Sort.by(asc("dateEcheance"), asc("id"));
            case SANS_SUITE -> Sort.by(desc("dateEcheance"), desc("id"));
            case TOUS -> Sort.by(desc("datePreparation"), desc("id"));
        };
    }
}