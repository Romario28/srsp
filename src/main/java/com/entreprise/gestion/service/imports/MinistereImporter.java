package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.Ministere;
import com.entreprise.gestion.repository.referentiel.MinistereRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MinistereImporter implements ImporteurExcel {

    private final MinistereRepository ministereRepository;

    @Override public String cle() { return "ministere"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {
        int crees = 0, misAJour = 0;
        List<String> erreurs = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);
            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;
                try {
                    String code = LectureExcel.normaliserCode(LectureExcel.texte(ligne, 0));
                    if (code == null || code.isBlank()) continue;

                    Ministere m = ministereRepository.findById(code).orElseGet(Ministere::new);
                    boolean nouveau = (m.getCode() == null);
                    m.setCode(code);
                    m.setLibelle(LectureExcel.texte(ligne, 1));
                    ministereRepository.save(m);

                    if (nouveau) crees++; else misAJour++;
                } catch (Exception e) {
                    erreurs.add("Ligne " + (ligne.getRowNum() + 1) + " : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            erreurs.add("Fichier illisible : " + e.getMessage());
        }
        return new RapportImport(crees + misAJour, crees, misAJour, erreurs);
    }
}