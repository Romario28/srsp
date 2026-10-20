package com.entreprise.gestion.service.exports;

import com.entreprise.gestion.service.imports.LectureExcel;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExportFichierTest {
    private static final int NB_BASE=23;
    private static final int COL_NAISSANCE=4;
    private static final int COL_PREPARATION=NB_BASE;
    private static final int COL_DETECTION=NB_BASE+1;
    private static final int COL_VIDE=NB_BASE+2;

    private XSSFWorkbook ecrireEtRelire() throws Exception {
        Object[] ligne=new Object[NB_BASE+3];
        ligne[1]="123456";
        ligne[COL_NAISSANCE]=LocalDate.of(1966,10,5);
        ligne[COL_PREPARATION]=LocalDate.of(2026,10,15);
        ligne[COL_DETECTION]=LocalDateTime.of(2026,10,15,14,30);
        ExportService.Fichier fichier=new ExportService.Fichier("x.xlsx",Collections.singletonList(ligne),
                List.of("DATE_PREPARATION","DATE_DETECTION","DATE_ACQUITTEMENT"));
        ByteArrayOutputStream octets=new ByteArrayOutputStream();
        fichier.ecrireDans(octets);
        return new XSSFWorkbook(new ByteArrayInputStream(octets.toByteArray()));
    }

    @Test void lesDatesSontDeVraiesDatesAffichees_jjmmaaaa() throws Exception {
        try(XSSFWorkbook wb=ecrireEtRelire()) {
            Row ligne=wb.getSheetAt(0).getRow(1);
            for(int colonne:new int[]{COL_NAISSANCE,COL_PREPARATION}) {
                Cell c=ligne.getCell(colonne);
                assertThat(c.getCellType()).isEqualTo(CellType.NUMERIC);
                assertThat(DateUtil.isCellDateFormatted(c)).isTrue();
                assertThat(c.getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy");
            }
            assertThat(ligne.getCell(COL_PREPARATION).getLocalDateTimeCellValue().toLocalDate()).isEqualTo(LocalDate.of(2026,10,15));
            assertThat(ligne.getCell(COL_DETECTION).getCellStyle().getDataFormatString()).isEqualTo("dd/mm/yyyy hh:mm");
            assertThat(ligne.getCell(COL_DETECTION).getLocalDateTimeCellValue()).isEqualTo(LocalDateTime.of(2026,10,15,14,30));
        }
    }

    @Test void lImportRelitLesDatesExportees() throws Exception {
        try(XSSFWorkbook wb=ecrireEtRelire()) {
            assertThat(LectureExcel.date(wb.getSheetAt(0).getRow(1),COL_NAISSANCE)).isEqualTo(LocalDate.of(1966,10,5));
        }
    }

    @Test void valeursNullesRestentVidesEtCodesEnTexte() throws Exception {
        try(XSSFWorkbook wb=ecrireEtRelire()) {
            Row ligne=wb.getSheetAt(0).getRow(1);
            assertThat(ligne.getCell(COL_VIDE)).isNull();
            assertThat(ligne.getCell(1).getStringCellValue()).isEqualTo("123456");
        }
    }

    @Test void lesColonnesSontAssezLargesPourUneDate() throws Exception {
        try(XSSFWorkbook wb=ecrireEtRelire()) {
            Sheet feuille=wb.getSheetAt(0);
            assertThat(feuille.getColumnWidth(COL_NAISSANCE)).isGreaterThanOrEqualTo(12*256);
            assertThat(feuille.getColumnWidth(COL_DETECTION)).isGreaterThanOrEqualTo(16*256);
        }
    }
}
