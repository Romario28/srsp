package com.entreprise.gestion.service.imports;

import org.apache.poi.ss.usermodel.*;
import java.time.LocalDate;
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
        Map<String, Integer> index = new HashMap<>();
        for (Cell cellule : entete) {
            index.put(cellule.getStringCellValue().trim().toUpperCase(), cellule.getColumnIndex());
        }
        return index;
    }

    public static int colonneObligatoire(Map<String, Integer> entetes, String nom) {
        Integer idx = entetes.get(nom.toUpperCase());
        if (idx == null) {
            throw new IllegalStateException("Colonne obligatoire absente du fichier : " + nom);
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