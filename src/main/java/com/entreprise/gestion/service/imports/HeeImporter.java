// HeeImporter.java — même patron
package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.Hee;
import com.entreprise.gestion.repository.referentiel.HeeRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class HeeImporter implements ImporteurExcel {

    private final HeeRepository heeRepository;

    @Override public String cle() { return "hee"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {
        int crees = 0, misAJour = 0;
        List<String> erreurs = new ArrayList<>();
        Map<String, Integer> occurrencesCode = new HashMap<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);
            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;
                try {
                    String code = LectureExcel.texte(ligne, 0);
                    if (code == null || code.isBlank()) continue;
                    occurrencesCode.merge(code, 1, Integer::sum);

                    Hee h = heeRepository.findById(code).orElseGet(Hee::new);
                    boolean nouveau = (h.getCode() == null);
                    h.setCode(code);
                    h.setLibelle(LectureExcel.texte(ligne, 1));
                    heeRepository.save(h);

                    if (nouveau) crees++; else misAJour++;
                } catch (Exception e) {
                    erreurs.add("Ligne " + (ligne.getRowNum() + 1) + " : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            erreurs.add("Fichier illisible : " + e.getMessage());
        }

        Map<String, Integer> doublons = occurrencesCode.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new RapportImport(crees + misAJour, crees, misAJour, erreurs, doublons, Map.of());
    }
}
