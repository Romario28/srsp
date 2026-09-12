package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.exception.BusinessException;
import org.springframework.stereotype.Service;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImportService {

    private final Map<String, ImporteurExcel> importeurs;

    public ImportService(List<ImporteurExcel> liste) {
        this.importeurs = liste.stream().collect(Collectors.toMap(ImporteurExcel::cle, i -> i));
    }

    public RapportImport importer(String type, InputStream fichier) {
        ImporteurExcel importeur = importeurs.get(type);
        if (importeur == null) {
            throw new BusinessException("TYPE_IMPORT_INCONNU", "Aucun importeur pour : " + type);
        }
        return importeur.importer(fichier);
    }
}