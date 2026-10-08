package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.CasGradeSuivant;

import java.util.List;

/** Grade supérieur déterminé, ambigu, terminal ou impossible à calculer. */
public record GradeSuivant(CasGradeSuivant cas, List<String> codes) {

    public static final GradeSuivant DERNIER_GRADE = new GradeSuivant(CasGradeSuivant.DERNIER_GRADE, List.of());
    public static final GradeSuivant INDETERMINE = new GradeSuivant(CasGradeSuivant.INDETERMINE, List.of());

    public GradeSuivant {
        codes = codes == null ? List.of() : List.copyOf(codes);
    }

    public static GradeSuivant unique(String code) {
        return new GradeSuivant(CasGradeSuivant.UNIQUE, List.of(code));
    }

    public static GradeSuivant ambigu(List<String> codes) {
        return new GradeSuivant(CasGradeSuivant.AMBIGU, codes);
    }

    /** Valeur compacte stockée dans la colonne grade_suivant. */
    public String valeurStockee() {
        return String.join(", ", codes);
    }

    public static GradeSuivant depuisStockage(CasGradeSuivant cas, String valeur) {
        if (cas == null) return null;
        if (valeur == null || valeur.isBlank()) return new GradeSuivant(cas, List.of());
        return new GradeSuivant(cas, List.of(valeur.split(",\\s*")));
    }
}
