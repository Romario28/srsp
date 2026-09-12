package com.entreprise.gestion.service.imports;

import org.apache.poi.ss.usermodel.Row;

import java.io.InputStream;

import static com.entreprise.gestion.service.imports.LectureExcel.texte;

public interface ImporteurExcel {
    String cle();
    RapportImport importer(InputStream fichier);


}