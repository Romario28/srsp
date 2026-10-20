package com.entreprise.gestion.service.exports;

import com.entreprise.gestion.exception.BusinessException;
import java.util.Arrays;

public enum Ecran {
    CALENDRIER("calendrier", "Calendrier"), RETRAITE("retraite", "Départs à la retraite"),
    AVANCEMENT("avancement", "Avancements"), TITULARISATION("titularisation", "Titularisations"),
    FIN_CONTRAT("fin-contrat", "Fins de contrat"), INTEGRATION("integration", "Intégrations"),
    ANOMALIES("anomalies", "Anomalies"), ALERTES("alertes", "Alertes"), DOSSIERS("dossiers", "Dossiers");
    private final String slug, libelle;
    Ecran(String slug, String libelle) { this.slug = slug; this.libelle = libelle; }
    public String slug() { return slug; }
    public String libelle() { return libelle; }
    public static Ecran depuisSlug(String slug) {
        return Arrays.stream(values()).filter(e -> e.slug.equals(slug)).findFirst()
                .orElseThrow(() -> new BusinessException("ECRAN_INCONNU", "Écran inconnu : " + slug));
    }
}
