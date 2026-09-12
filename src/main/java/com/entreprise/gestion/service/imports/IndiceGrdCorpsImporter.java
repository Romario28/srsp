package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.repository.referentiel.CorpsRepository;
import com.entreprise.gestion.repository.referentiel.GradeRepository;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class IndiceGrdCorpsImporter implements ImporteurExcel {

    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;
    private final GradeRepository gradeRepository;
    private final CorpsRepository corpsRepository;

    @Override public String cle() { return "indice-grade-corps"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {
        int crees = 0, misAJour = 0, compteur = 0;
        List<String> erreurs = new ArrayList<>();
        Map<String, Integer> nonResolus = new HashMap<>();
        Map<String, List<String>> exemplesNonResolus = new HashMap<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);
            Map<String, Integer> col = LectureExcel.indexerEntetes(feuille);

            int idxGrade     = LectureExcel.colonneObligatoire(col, "GRADE_CODE");
            int idxCorps     = LectureExcel.colonneObligatoire(col, "CORPS_CODE");
            int idxCategorie = LectureExcel.colonneObligatoire(col, "CATEGORIE_CODE");
            int idxIndice    = LectureExcel.colonneObligatoire(col, "INDICE");
            Integer idxDuree = col.get("DUREE_REQUISE");

            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;
                try {
                    String gradeCode     = LectureExcel.texte(ligne, idxGrade);
                    String corpsCode     = LectureExcel.texte(ligne, idxCorps);
                    String categorieCode = LectureExcel.normaliserCode(LectureExcel.texte(ligne, idxCategorie));
                    String indice        = LectureExcel.texte(ligne, idxIndice);
                    Integer duree        = idxDuree != null ? LectureExcel.entier(ligne, idxDuree) : null;

                    if (gradeCode == null || corpsCode == null || categorieCode == null) continue;

                    Grade grade = gradeRepository.findById(gradeCode).orElse(null);
                    Corps corps = corpsRepository.findById(new CorpsId(corpsCode, categorieCode)).orElse(null);

                    if (grade == null) {
                        signalerAbsence("grade_code", gradeCode, nonResolus, exemplesNonResolus);
                        continue;
                    }
                    if (corps == null) {
                        signalerAbsence("corps_code+categorie", corpsCode + "|" + categorieCode,
                                nonResolus, exemplesNonResolus);
                        continue;
                    }

//                    IndiceGrdCorpsId id = new IndiceGrdCorpsId(gradeCode, corps.getCode() != null ? new CorpsId(corps.getCode(), corps.getCategorie()) : null);
                    IndiceGrdCorpsId id = new IndiceGrdCorpsId(gradeCode, new CorpsId(corps.getCode(), corps.getCategorie()));
                    IndiceGrdCorps entite = indiceGrdCorpsRepository.findById(id).orElseGet(() -> {
                        IndiceGrdCorps n = new IndiceGrdCorps();
                        n.setGrade(grade);
                        n.setCorps(corps);
                        return n;
                    });
                    boolean nouveau = (entite.getIndice() == null);
                    entite.setIndice(indice);
                    if (duree != null) entite.setDureeRequise(duree);
                    indiceGrdCorpsRepository.save(entite);

                    if (nouveau) crees++; else misAJour++;
                    if (++compteur % 500 == 0) indiceGrdCorpsRepository.flush();
                } catch (Exception e) {
                    erreurs.add("Ligne " + (ligne.getRowNum() + 1) + " : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            erreurs.add("Fichier illisible : " + e.getMessage());
        }
        return new RapportImport(crees + misAJour, crees, misAJour, erreurs, Map.of(), nonResolus, exemplesNonResolus);
    }

    private void signalerAbsence(String champ, String valeur, Map<String, Integer> nonResolus,
                                 Map<String, List<String>> exemples) {
        nonResolus.merge(champ, 1, Integer::sum);
        List<String> liste = exemples.computeIfAbsent(champ, k -> new ArrayList<>());
        if (liste.size() < 5 && !liste.contains(valeur)) liste.add(valeur);
    }
}

/*
package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.CorpsId;
import com.entreprise.gestion.entite.anticipation.IndiceGrdCorps;
import com.entreprise.gestion.entite.anticipation.IndiceGrdCorpsId;
import com.entreprise.gestion.repository.referentiel.CorpsRepository;
import com.entreprise.gestion.repository.referentiel.GradeRepository;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class IndiceGrdCorpsImporter implements ImporteurExcel {

    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;
    private final GradeRepository gradeRepository;
    private final CorpsRepository corpsRepository;

    @Override public String cle() { return "indice-grade-corps"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {
        int crees = 0, misAJour = 0, compteur = 0;
        List<String> erreurs = new ArrayList<>();
        Map<String, Integer> nonResolus = new HashMap<>();
        Map<String, List<String>> exemplesNonResolus = new HashMap<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);
            Map<String, Integer> col = LectureExcel.indexerEntetes(feuille);

            int idxGrade     = LectureExcel.colonneObligatoire(col, "GRADE_CODE");
            int idxCorps     = LectureExcel.colonneObligatoire(col, "CORPS_CODE");
            int idxCategorie = LectureExcel.colonneObligatoire(col, "CATEGORIE_CODE");
            int idxIndice    = LectureExcel.colonneObligatoire(col, "INDICE");
            Integer idxDuree = col.get("DUREE_REQUISE");   // optionnel : selon la version du fichier

            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;
                try {
                    String gradeCode     = LectureExcel.texte(ligne, idxGrade);
                    String corpsCode     = LectureExcel.texte(ligne, idxCorps);
                    String categorieCode = LectureExcel.texte(ligne, idxCategorie);
                    String indice        = LectureExcel.texte(ligne, idxIndice);
                    Integer duree        = idxDuree != null ? LectureExcel.entier(ligne, idxDuree) : null;

                    if (gradeCode == null || corpsCode == null || categorieCode == null) continue;

                    CorpsId corpsId = new CorpsId(corpsCode, categorieCode);
                    IndiceGrdCorpsId id = new IndiceGrdCorpsId(gradeCode, corpsId);

                    IndiceGrdCorps entite = indiceGrdCorpsRepository.findById(id).orElseGet(() -> {
                        IndiceGrdCorps n = new IndiceGrdCorps();
                        n.setGrade(gradeRepository.getReferenceById(gradeCode));
                        n.setCorps(corpsRepository.getReferenceById(corpsId));
                        return n;
                    });
                    boolean nouveau = (entite.getIndice() == null);
                    entite.setIndice(indice);
                    if (duree != null) entite.setDureeRequise(duree);   // ne jamais écraser par null
                    indiceGrdCorpsRepository.save(entite);

                    if (nouveau) crees++; else misAJour++;
                    if (++compteur % 500 == 0) indiceGrdCorpsRepository.flush();
                } catch (Exception e) {
                    erreurs.add("Ligne " + (ligne.getRowNum() + 1) + " : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            erreurs.add("Fichier illisible : " + e.getMessage());
        }
        return new RapportImport(crees + misAJour, crees, misAJour, erreurs);
    }
}*/
