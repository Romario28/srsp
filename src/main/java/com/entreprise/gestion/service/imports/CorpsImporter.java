// CorpsImporter.java — trois colonnes : code, categorie, libelle
package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.Corps;
import com.entreprise.gestion.entite.anticipation.CorpsId;
import com.entreprise.gestion.repository.referentiel.CorpsRepository;
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
public class CorpsImporter implements ImporteurExcel {

    private final CorpsRepository corpsRepository;

    @Override public String cle() { return "corps"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {
        int crees = 0, misAJour = 0;
        List<String> erreurs = new ArrayList<>();
        Map<String, Integer> occurrencesCode = new HashMap<>();
        Map<String, Integer> nonResolus = new HashMap<>();
        Map<String, List<String>> exemplesNonResolus = new HashMap<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);

            Map<String, Integer> col = LectureExcel.indexerEntetes(feuille);
            int idxCorps     = LectureExcel.colonneObligatoire(col, "corps");
            int idxLibelle   = LectureExcel.colonneObligatoire(col, "libelle");
            int idxCategorie = LectureExcel.colonneObligatoire(col, "categorie");

            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;
                try {
                    String code       = LectureExcel.texte(ligne, idxCorps);
                    String libelle    = LectureExcel.texte(ligne, idxLibelle);
                    String categorie  = LectureExcel.normaliserCode(LectureExcel.texte(ligne, idxCategorie));

                    if (code == null || code.isBlank() || categorie == null || categorie.isBlank()) continue;

                    String cleComposite = code + "|" + categorie;
                    occurrencesCode.merge(cleComposite, 1, Integer::sum);

                    CorpsId id = new CorpsId(code, categorie);
                    Corps c = corpsRepository.findById(id).orElseGet(Corps::new);
                    boolean nouveau = (c.getCode() == null);

                    c.setCode(code);
                    c.setCategorie(categorie);
                    c.setLibelle(libelle);
                    corpsRepository.save(c);

                    if (nouveau) crees++;
                    else misAJour++;
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

        return new RapportImport(crees + misAJour, crees, misAJour, erreurs, doublons,nonResolus ,exemplesNonResolus );
    }
}