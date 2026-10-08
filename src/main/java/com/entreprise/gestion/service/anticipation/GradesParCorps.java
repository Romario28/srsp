package com.entreprise.gestion.service.anticipation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/** Index des grades ordonnés par indice, séparément pour chaque corps et catégorie. */
public final class GradesParCorps {

    private record Niveau(Map<String, Set<BigDecimal>> indicesParGrade) {}

    private final Map<String, Niveau> parCorps = new HashMap<>();
    private final RenommagesGrades renommages;

    GradesParCorps(List<LigneIndice> lignes, Set<String> gradesStagiaire, RenommagesGrades renommages) {
        this.renommages = renommages;
        Map<String, Map<String, Set<BigDecimal>>> index = new HashMap<>();
        for (LigneIndice ligne : lignes) {
            BigDecimal indice = lireIndice(ligne.indice());
            if (indice == null || ligne.gradeCode() == null || ligne.corpsCode() == null || ligne.categorie() == null) continue;
            String cleCorps = cle(ligne.corpsCode(), ligne.categorie());
            String grade = renommages.appliquer(ligne.corpsCode(), ligne.gradeCode());
            index.computeIfAbsent(cleCorps, k -> new HashMap<>())
                    .computeIfAbsent(grade, k -> new HashSet<>())
                    .add(indice);
        }
        index.forEach((cle, grades) -> parCorps.put(cle, new Niveau(grades)));
    }

    public GradeSuivant pour(String corpsCode, String categorie, String gradeCode) {
        Niveau niveau = parCorps.get(cle(corpsCode, categorie));
        if (niveau == null) return GradeSuivant.INDETERMINE;

        String grade = renommages.appliquer(corpsCode, gradeCode);
        Set<BigDecimal> indicesCourants = niveau.indicesParGrade().get(grade);
        if (indicesCourants == null || indicesCourants.size() != 1) return GradeSuivant.INDETERMINE;
        BigDecimal indiceCourant = indicesCourants.iterator().next();

        BigDecimal indiceSuivant = niveau.indicesParGrade().values().stream()
                .flatMap(Set::stream)
                .filter(indice -> indice.compareTo(indiceCourant) > 0)
                .min(BigDecimal::compareTo)
                .orElse(null);
        if (indiceSuivant == null) return GradeSuivant.DERNIER_GRADE;

        List<String> candidats = niveau.indicesParGrade().entrySet().stream()
                .filter(e -> e.getValue().stream().anyMatch(i -> i.compareTo(indiceSuivant) == 0))
                .map(Map.Entry::getKey)
                .sorted(Comparator.naturalOrder())
                .toList();
        return candidats.size() == 1 ? GradeSuivant.unique(candidats.get(0)) : GradeSuivant.ambigu(candidats);
    }

    private static String cle(String corpsCode, String categorie) {
        return corpsCode + "|" + categorie;
    }

    private static BigDecimal lireIndice(String texte) {
        if (texte == null || texte.isBlank()) return null;
        try {
            return new BigDecimal(texte.replaceAll("\\s+", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
