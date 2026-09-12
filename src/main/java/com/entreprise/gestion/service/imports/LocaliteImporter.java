// LocaliteImporter.java — dédoublonnage sur code_localite, sans province
package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.Localite;
import com.entreprise.gestion.repository.referentiel.LocaliteRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Component
@RequiredArgsConstructor
public class LocaliteImporter implements ImporteurExcel {

    private final LocaliteRepository localiteRepository;

    @Override public String cle() { return "localite"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {
        Map<String, String> dedupliques = new LinkedHashMap<>();
        List<String> erreurs = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);
            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;
                try {
                    String nom  = LectureExcel.texte(ligne, 0);
                    String code = LectureExcel.texte(ligne, 1);
                    if (code == null || code.isBlank()) continue;
                    dedupliques.putIfAbsent(code, nom);
                } catch (Exception e) {
                    erreurs.add("Ligne " + (ligne.getRowNum() + 1) + " : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            erreurs.add("Fichier illisible : " + e.getMessage());
        }

        int crees = 0, misAJour = 0;
        for (var entree : dedupliques.entrySet()) {
            Localite l = localiteRepository.findById(entree.getKey()).orElseGet(Localite::new);
            boolean nouveau = (l.getCode() == null);
            l.setCode(entree.getKey());
            l.setNom(entree.getValue());
            localiteRepository.save(l);
            if (nouveau) crees++; else misAJour++;
        }
        return new RapportImport(dedupliques.size(), crees, misAJour, erreurs);
    }
}