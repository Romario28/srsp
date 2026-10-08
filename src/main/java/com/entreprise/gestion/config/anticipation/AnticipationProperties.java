package com.entreprise.gestion.config.anticipation;

import com.entreprise.gestion.service.anticipation.RenommagesGrades;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/** Règles métier d'anticipation configurées dans application.yaml. */
@Slf4j
@Component
@ConfigurationProperties(prefix = "anticipation")
@Getter
@Setter
public class AnticipationProperties {

    /** Ancien code → nouveau code, valable dans tous les corps. */
    private Map<String, String> correspondanceGrades = new HashMap<>();

    /** Code corps → (ancien code → nouveau code), prioritaire sur la table globale. */
    private Map<String, Map<String, String>> correspondanceGradesParCorps = new HashMap<>();

    public RenommagesGrades renommages() {
        return new RenommagesGrades(correspondanceGrades, correspondanceGradesParCorps);
    }

    @PostConstruct
    void verifier() {
        verifierPaires("anticipation.correspondance-grades", correspondanceGrades);
        correspondanceGradesParCorps.forEach((corps, locaux) -> {
            if (corps == null || corps.isBlank()) {
                throw new IllegalStateException("anticipation.correspondance-grades-par-corps : code corps vide.");
            }
            Map<String, String> effectives = new HashMap<>(correspondanceGrades);
            effectives.putAll(locaux);
            verifierPaires("anticipation.correspondance-grades-par-corps." + corps, effectives);
        });
        log.info("Correspondance de grades : {} renommage(s) global(aux), {} corps avec renommages propres",
                correspondanceGrades.size(), correspondanceGradesParCorps.size());
    }

    private static void verifierPaires(String source, Map<String, String> paires) {
        paires.forEach((ancien, nouveau) -> {
            String paire = "« " + ancien + " → " + nouveau + " »";
            if (ancien == null || ancien.isBlank() || nouveau == null || nouveau.isBlank()) {
                throw new IllegalStateException(source + " : code vide dans " + paire);
            }
            if (ancien.equals(nouveau)) {
                throw new IllegalStateException(source + " : " + paire + " ne renomme rien.");
            }
            if (paires.containsKey(nouveau)) {
                throw new IllegalStateException(source + " : " + paire + " est en chaîne (" + nouveau
                        + " est lui-même renommé en " + paires.get(nouveau) + "). Indiquez directement le dernier nom.");
            }
        });
    }
}
