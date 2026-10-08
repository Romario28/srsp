package com.entreprise.gestion.service.anticipation;

import java.util.Map;

/** Correspondances ancien → nouveau code, globales ou propres à un corps. */
public record RenommagesGrades(Map<String, String> globaux, Map<String, Map<String, String>> parCorps) {

    public static final RenommagesGrades AUCUN = new RenommagesGrades(Map.of(), Map.of());

    public RenommagesGrades {
        globaux = globaux == null ? Map.of() : Map.copyOf(globaux);
        parCorps = parCorps == null ? Map.of()
                : parCorps.entrySet().stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey, e -> Map.copyOf(e.getValue())));
    }

    /** Applique une seule règle ; celle du corps prime sur la globale. */
    public String appliquer(String corpsCode, String grade) {
        if (grade == null) return null;
        Map<String, String> locaux = parCorps.get(corpsCode);
        if (locaux != null && locaux.containsKey(grade)) return locaux.get(grade);
        return globaux.getOrDefault(grade, grade);
    }
}
