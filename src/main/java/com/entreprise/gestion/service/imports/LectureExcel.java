package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.exception.BusinessException;
import org.apache.poi.ss.usermodel.*;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public final class LectureExcel {

    public static String texte(Row ligne, int colonne) {
        Cell cellule = ligne.getCell(colonne);
        if (cellule == null) return null;
        return switch (cellule.getCellType()) {
            case STRING  -> cellule.getStringCellValue().trim();
            case NUMERIC -> String.valueOf((long) cellule.getNumericCellValue());
            default -> null;
        };
    }

    public static LocalDate date(Row ligne, int colonne) {
        Cell cellule = ligne.getCell(colonne);
        if (cellule == null) return null;
        try {
            if (cellule.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cellule)) {
                return cellule.getLocalDateTimeCellValue().toLocalDate();
            }
            String texte = texte(ligne, colonne);
            return texte != null ? LocalDate.parse(texte) : null;
        } catch (Exception e) {
            return null;
        }
    }

    
    public static Map<String, Integer> indexerEntetes(Sheet feuille) {
        Row entete = feuille.getRow(0);
        if (entete == null) {
            throw new BusinessException("FICHIER_SANS_ENTETES",
                    "La première ligne de la première feuille est vide : elle doit contenir les en-têtes de colonnes.");
        }
        Map<String, Integer> index = new HashMap<>();
        for (Cell cellule : entete) {
            index.put(cellule.getStringCellValue().trim().toUpperCase(), cellule.getColumnIndex());
        }
        return index;
    }

    public static void exigerColonnes(Map<String, Integer> entetes, String... noms) {
        List<String> manquantes = Arrays.stream(noms)
                .filter(n -> !entetes.containsKey(n.toUpperCase()))
                .toList();
        if (manquantes.isEmpty()) return;
        throw new BusinessException("COLONNES_ABSENTES",
                (manquantes.size() == 1 ? "Colonne obligatoire absente du fichier : "
                        : "Colonnes obligatoires absentes du fichier : ") + String.join(", ", manquantes));
    }

    public static int colonneObligatoire(Map<String, Integer> entetes, String nom) {
        Integer idx = entetes.get(nom.toUpperCase());
        if (idx == null) {
            throw new BusinessException("COLONNE_ABSENTE", "Colonne obligatoire absente du fichier : " + nom);
        }
        return idx;
    }

    public static Integer entier(Row ligne, int colonne) {
        String texte = texte(ligne, colonne);
        if (texte == null || texte.isBlank()) return null;
        try { return Integer.parseInt(texte.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    public static String normaliserCode(String brut) {
        if (brut == null) return null;
        String nettoye = brut.trim();
        if (nettoye.isEmpty()) return null;
        return nettoye.matches("\\d+") && nettoye.length() == 1
                ? "0" + nettoye
                : nettoye;
    }

/*    public static String normaliserCategorie(String brut) {
        if (brut == null || brut.isBlank()) return brut;
        String trim = brut.trim();
        if (!trim.matches("\\d+")) return trim;
        return String.format("%02d", Integer.parseInt(trim));
    }*/

    private LectureExcel() {}
}
