package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.entite.anticipation.Soa;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.repository.referentiel.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentImporter implements ImporteurExcel {

    private static final int TAILLE_LOT = 500;

    private final AgentRepository       agentRepository;
    private final CorpsRepository       corpsRepository;
    private final GradeRepository       gradeRepository;
    private final SanctionRepository    sanctionRepository;
    private final SoaRepository         soaRepository;
    private final LocaliteRepository    localiteRepository;
    private final MinistereRepository   ministereRepository;
    private final HeeRepository         heeRepository;
    private final EntityManager         entityManager;

    @Override public String cle() { return "agents"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {

        Map<String, Corps>     corpsParCle      = index(corpsRepository.findAll(), c -> cleCorps(c.getCode(), c.getCategorie()));
        Map<String, Grade>     gradeParCode     = index(gradeRepository.findAll(), Grade::getCode);
        Map<String, Sanction>  sanctionParCode  = index(sanctionRepository.findAll(), Sanction::getCode);
        Map<String, Soa>       soaParCode       = index(soaRepository.findAll(), Soa::getCode);
        Map<String, Localite>  localiteParCode  = index(localiteRepository.findAll(), Localite::getCode);
        Map<String, Ministere> ministereParCode = index(ministereRepository.findAll(), Ministere::getCode);
        Map<String, Hee>       heeParCode       = index(heeRepository.findAll(), Hee::getCode);

        Map<String, Integer> occurrencesMatricule = new HashMap<>();
        Map<String, Integer> nonResolus = new HashMap<>();
        Map<String, List<String>> exemplesNonResolus = new HashMap<>();

        int crees = 0, misAJour = 0, compteur = 0;
        List<String> erreurs = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);
            Map<String, Integer> col = LectureExcel.indexerEntetes(feuille);

            int idxPoste        = LectureExcel.colonneObligatoire(col, "POSTE_AGENT_NUMERO");
            int idxMatricule    = LectureExcel.colonneObligatoire(col, "AGENT_MATRICULE");
            int idxNom          = LectureExcel.colonneObligatoire(col, "AGENT_NOM");
            int idxPrenoms      = LectureExcel.colonneObligatoire(col, "AGENT_PRENOMS");
            int idxDateNais     = LectureExcel.colonneObligatoire(col, "AGENT_DATE_NAIS");
            int idxCin          = LectureExcel.colonneObligatoire(col, "AGENT_CIN");
            int idxSexe         = LectureExcel.colonneObligatoire(col, "AGENT_SEXE");
            int idxStatut       = LectureExcel.colonneObligatoire(col, "STATUT");
            int idxCorps        = LectureExcel.colonneObligatoire(col, "CORPS_CODE");
            int idxGrade        = LectureExcel.colonneObligatoire(col, "GRADE_CODE");
            int idxCategorie    = LectureExcel.colonneObligatoire(col, "CATEGORIE");
            int idxIndice       = LectureExcel.colonneObligatoire(col, "INDICE");
            int idxHee          = LectureExcel.colonneObligatoire(col, "HEE_CODE");
            int idxHeeCat       = LectureExcel.colonneObligatoire(col, "HEE_CATEGORIE_CODE");
            int idxSection      = LectureExcel.colonneObligatoire(col, "SECTION_CODE");
            int idxFiv          = LectureExcel.colonneObligatoire(col, "FIV_CODE");
            int idxSanction     = LectureExcel.colonneObligatoire(col, "SANCTION_CODE");
            int idxSoa          = LectureExcel.colonneObligatoire(col, "SOA");
            int idxDebutContrat = LectureExcel.colonneObligatoire(col, "POSTE_AGENT_DATE_DEBUT_CONTRAT");
            int idxFinContrat   = LectureExcel.colonneObligatoire(col, "POSTE_AGENT_DATE_FIN_CONTRAT");
            int idxAvanceDate   = LectureExcel.colonneObligatoire(col, "AVANCE_DATE");
            int idxRegCode      = LectureExcel.colonneObligatoire(col, "REG_CODE");
            int idxMinCode      = LectureExcel.colonneObligatoire(col, "MIN_CODE");

            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;

                String matricule = LectureExcel.texte(ligne, idxMatricule);
                String cin = LectureExcel.texte(ligne, idxCin);
                try {
                    if (matricule == null || matricule.isBlank()) continue;

                    occurrencesMatricule.merge(matricule, 1, Integer::sum);

                    Agent a = agentRepository.findById(matricule).orElseGet(Agent::new);
                    boolean nouveau = (a.getNom() == null);

                    a.setMatricule(matricule);
                    a.setCin(cin);
                    a.setNom(LectureExcel.texte(ligne, idxNom));
                    a.setPrenoms(LectureExcel.texte(ligne, idxPrenoms));
                    a.setDateNaissance(LectureExcel.date(ligne, idxDateNais));
                    a.setSexe(LectureExcel.texte(ligne, idxSexe));
                    a.setStatut(parseStatut(LectureExcel.texte(ligne, idxStatut)));

                    a.setCorps(resoudre(
                            cleCorps(LectureExcel.texte(ligne, idxCorps), LectureExcel.texte(ligne, idxCategorie)),
                            corpsParCle, "corps_code+categorie", nonResolus, exemplesNonResolus));
                    a.setGrade(resoudre(LectureExcel.texte(ligne, idxGrade), gradeParCode,
                            "grade_code", nonResolus, exemplesNonResolus));
                    a.setIndiceActuel(LectureExcel.texte(ligne, idxIndice));
                    a.setHee(resoudre(LectureExcel.texte(ligne, idxHee), heeParCode,
                            "hee_code", nonResolus, exemplesNonResolus));
                    a.setHeeCategorieCode(LectureExcel.texte(ligne, idxHeeCat));
                    a.setSectionCode(LectureExcel.texte(ligne, idxSection));
                    a.setLocalite(resoudre(LectureExcel.texte(ligne, idxFiv), localiteParCode,
                            "fiv_code", nonResolus, exemplesNonResolus));
                    a.setSanction(resoudre(LectureExcel.texte(ligne, idxSanction), sanctionParCode,
                            "sanction_code", nonResolus, exemplesNonResolus));
                    a.setSoa(resoudre(LectureExcel.texte(ligne, idxSoa), soaParCode,
                            "soa", nonResolus, exemplesNonResolus));

                    a.setDateDebutContrat(LectureExcel.date(ligne, idxDebutContrat));
                    a.setDateFinContrat(LectureExcel.date(ligne, idxFinContrat));
                    a.setAvanceDate(LectureExcel.date(ligne, idxAvanceDate));

                    a.setPosteNumero(LectureExcel.texte(ligne, idxPoste));
                    a.setRegCode(LectureExcel.texte(ligne, idxRegCode));
                    a.setMinistere(resoudre(LectureExcel.texte(ligne, idxMinCode), ministereParCode,
                            "min_code", nonResolus, exemplesNonResolus));

                    agentRepository.save(a);
                    if (nouveau) crees++; else misAJour++;

                    if (++compteur % TAILLE_LOT == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                } catch (Exception e) {
                    erreurs.add("Ligne " + (ligne.getRowNum() + 1)
                            + " (matricule " + matricule + ", cin " + cin + ") : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            erreurs.add("Fichier illisible : " + e.getMessage());
        }

        Map<String, Integer> doublons = occurrencesMatricule.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new RapportImport(crees + misAJour, crees, misAJour, erreurs,
                doublons, nonResolus, exemplesNonResolus);
    }

    private <T> T resoudre(String codeBrut, Map<String, T> index, String nomChamp,
                           Map<String, Integer> nonResolus, Map<String, List<String>> exemples) {
        if (codeBrut == null || codeBrut.isBlank()) return null;
        T valeur = index.get(codeBrut);
        if (valeur == null) {
            nonResolus.merge(nomChamp, 1, Integer::sum);
//            List<String> liste = exemples.computeIfAbsent(nomChamp, k -> new ArrayList<>());
//            if (!liste.contains(codeBrut)) liste.add(codeBrut);

            List<String> liste = exemples.computeIfAbsent(nomChamp, k -> new ArrayList<>());
            if (liste.size() < 5 && !liste.contains(codeBrut)) liste.add(codeBrut);
        }
        return valeur;
    }

    private <T> Map<String, T> index(List<T> liste, Function<T, String> cle) {
        return liste.stream()
                .filter(e -> cle.apply(e) != null)
                .collect(Collectors.toMap(cle, Function.identity(), (a, b) -> a));
    }

    private static String cleCorps(String code, String categorie) {
        if (code == null || code.isBlank() || categorie == null || categorie.isBlank()) return null;
        String catNorm = LectureExcel.normaliserCode(categorie);
        if (catNorm == null || catNorm.isBlank()) return null;
        return code.trim() + "|" + catNorm;
    }

    private StatutAgent parseStatut(String brut) {
        if (brut == null) return null;
        try { return StatutAgent.valueOf(brut.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}

/*
package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.repository.referentiel.*;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AgentImporter implements ImporteurExcel {

    private static final int TAILLE_LOT = 500;

    private final AgentRepository       agentRepository;
    private final CorpsRepository       corpsRepository;
    private final GradeRepository       gradeRepository;
    private final SanctionRepository    sanctionRepository;
    private final SoaRepository         soaRepository;
    private final LocaliteRepository    localiteRepository;
    private final MinistereRepository   ministereRepository;
    private final HeeRepository         heeRepository;
    private final EntityManager         entityManager;

    @Override public String cle() { return "agents"; }

    @Override
    @Transactional
    public RapportImport importer(InputStream fichier) {

        Map<String, Corps>     corpsParCode     = index(corpsRepository.findAll(), Corps::getCode);
        Map<String, Grade>     gradeParCode     = index(gradeRepository.findAll(), Grade::getCode);
        Map<String, Sanction>  sanctionParCode  = index(sanctionRepository.findAll(), Sanction::getCode);
        Map<String, Soa>       soaParCode       = index(soaRepository.findAll(), Soa::getCode);
        Map<String, Localite>  localiteParCode  = index(localiteRepository.findAll(), Localite::getCode);
        Map<String, Ministere> ministereParCode = index(ministereRepository.findAll(), Ministere::getCode);
        Map<String, Hee>       heeParCode       = index(heeRepository.findAll(), Hee::getCode);

        Map<String, Integer> occurrencesMatricule = new HashMap<>();   // détecte les doublons DANS ce fichier
        Map<String, Integer> nonResolus = new HashMap<>();             // code présent mais introuvable, par champ

        int crees = 0, misAJour = 0, compteur = 0;
        List<String> erreurs = new ArrayList<>();

        try (Workbook wb = WorkbookFactory.create(fichier)) {
            Sheet feuille = wb.getSheetAt(0);

            for (Row ligne : feuille) {
                if (ligne.getRowNum() == 0) continue;

                String matricule = LectureExcel.texte(ligne, 1);
                try {
                    if (matricule == null || matricule.isBlank()) continue;

                    occurrencesMatricule.merge(matricule, 1, Integer::sum);

                    Agent a = agentRepository.findById(matricule).orElseGet(Agent::new);
                    boolean nouveau = (a.getNom() == null);

                    a.setMatricule(matricule);
                    a.setNom(LectureExcel.texte(ligne, 2));
                    a.setPrenoms(LectureExcel.texte(ligne, 3));
                    a.setDateNaissance(LectureExcel.date(ligne, 4));
                    a.setCin(LectureExcel.texte(ligne, 5));
                    a.setSexe(LectureExcel.texte(ligne, 6));
                    a.setStatut(parseStatut(LectureExcel.texte(ligne, 7)));

                    a.setCorps(resoudre(LectureExcel.texte(ligne, 8), corpsParCode, "corps_code", nonResolus));
                    a.setGrade(resoudre(LectureExcel.texte(ligne, 9), gradeParCode, "grade_code", nonResolus));
                    a.setCategorieCode(LectureExcel.texte(ligne, 10));
                    a.setIndiceActuel(LectureExcel.texte(ligne, 11));
                    a.setHee(resoudre(LectureExcel.texte(ligne, 12), heeParCode, "hee_code", nonResolus));
                    a.setHeeCategorieCode(LectureExcel.texte(ligne, 13));
                    a.setSectionCode(LectureExcel.texte(ligne, 14));
                    a.setLocalite(resoudre(LectureExcel.texte(ligne, 15), localiteParCode, "fiv_code", nonResolus));
                    a.setSanction(resoudre(LectureExcel.texte(ligne, 16), sanctionParCode, "sanction_code", nonResolus));
                    a.setSoa(resoudre(LectureExcel.texte(ligne, 17), soaParCode, "soa", nonResolus));

                    a.setDateDebutContrat(LectureExcel.date(ligne, 18));
                    a.setDateFinContrat(LectureExcel.date(ligne, 19));
                    a.setAvanceDate(LectureExcel.date(ligne, 20));

                    a.setPosteNumero(LectureExcel.texte(ligne, 0));
                    a.setRegCode(LectureExcel.texte(ligne, 21));
                    a.setMinistere(resoudre(LectureExcel.texte(ligne, 22), ministereParCode, "min_code", nonResolus));

                    agentRepository.save(a);
                    if (nouveau) crees++; else misAJour++;

                    if (++compteur % TAILLE_LOT == 0) {
                        entityManager.flush();
                        entityManager.clear();
                    }
                } catch (Exception e) {
                    erreurs.add("Ligne " + (ligne.getRowNum() + 1)
                            + " (matricule " + matricule + ") : " + e.getMessage());
                }
            }
        } catch (IOException e) {
            erreurs.add("Fichier illisible : " + e.getMessage());
        }

        Map<String, Integer> doublons = occurrencesMatricule.entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        return new RapportImport(crees + misAJour, crees, misAJour, erreurs, doublons, nonResolus);
    }

    private <T> T resoudre(String codeBrut, Map<String, T> index, String nomChamp,
                           Map<String, Integer> nonResolus) {
        if (codeBrut == null || codeBrut.isBlank()) return null;   // champ vide dans le fichier : normal, pas compté
        T valeur = index.get(codeBrut);
        if (valeur == null) nonResolus.merge(nomChamp, 1, Integer::sum);   // code présent mais absent du référentiel
        return valeur;
    }

    private <T> Map<String, T> index(List<T> liste, Function<T, String> cle) {
        return liste.stream().collect(Collectors.toMap(cle, Function.identity(), (a, b) -> a));
    }

    private StatutAgent parseStatut(String brut) {
        if (brut == null) return null;
        try { return StatutAgent.valueOf(brut.trim().toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}*/
