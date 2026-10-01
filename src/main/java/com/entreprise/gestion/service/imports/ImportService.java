package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ImportService {

    private final Map<String, ImporteurExcel> importeurs;
    private final AuditService auditService;

    public ImportService(List<ImporteurExcel> liste, AuditService auditService) {
        this.importeurs = liste.stream().collect(Collectors.toMap(ImporteurExcel::cle, i -> i));
        this.auditService = auditService;
    }

    @Transactional
    public RapportImport importer(String type, InputStream fichier, String nomFichier, UserDetailsImpl actor) {
        ImporteurExcel importeur = importeurs.get(type);
        if (importeur == null) {
            throw new BusinessException("TYPE_IMPORT_INCONNU", "Aucun importeur pour : " + type);
        }
        RapportImport rapport = importeur.importer(fichier);
        auditService.logAvecEntite(actor.getUtilisateur(), "IMPORT_EXCEL", decrire(type, nomFichier, rapport), null);
        return rapport;
    }

    private static String decrire(String type, String nomFichier, RapportImport r) {
        StringBuilder sb = new StringBuilder("Import « ").append(type).append(" » — fichier « ")
                .append(nomFichierSur(nomFichier)).append(" » : ")
                .append(r.lus()).append(" traitées (").append(r.crees()).append(" créées, ")
                .append(r.misAJour()).append(" mises à jour)");
        if (!r.erreurs().isEmpty()) sb.append(", ").append(r.erreurs().size()).append(" erreur(s)");
        if (!r.doublonsCle().isEmpty()) sb.append(", ").append(r.doublonsCle().size()).append(" clé(s) en double");
        int references = r.referencesNonResolues().values().stream().mapToInt(Integer::intValue).sum();
        if (references > 0) sb.append(", ").append(references).append(" référence(s) inconnue(s)");
        return sb.toString();
    }

    private static String nomFichierSur(String nom) {
        if (nom == null || nom.isBlank()) return "(sans nom)";
        String propre = nom.replaceAll("[\\r\\n]+", " ").trim();
        return propre.length() <= 120 ? propre : propre.substring(0, 117) + "…";
    }
}
