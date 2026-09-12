
package com.entreprise.gestion.service.imports;

import java.util.List;
import java.util.Map;

public record RapportImport(
        int lus, int crees, int misAJour,
        List<String> erreurs,
        Map<String, Integer> doublonsCle,
        Map<String, Integer> referencesNonResolues,
        Map<String, List<String>> exemplesNonResolus
) {
    public RapportImport(int lus, int crees, int misAJour, List<String> erreurs) {
        this(lus, crees, misAJour, erreurs, Map.of(), Map.of(), Map.of());
    }

    public RapportImport(int lus, int crees, int misAJour, List<String> erreurs,
                         Map<String, Integer> doublonsCle, Map<String, Integer> referencesNonResolues) {
        this(lus, crees, misAJour, erreurs, doublonsCle, referencesNonResolues, Map.of());
    }
}

/*
package com.entreprise.gestion.service.imports;

import java.util.List;
import java.util.Map;

public record RapportImport(
        int lus,
        int crees,
        int misAJour,
        List<String> erreurs,
        Map<String, Integer> doublonsCle,           // clé apparue plusieurs fois DANS ce fichier
        Map<String, Integer> referencesNonResolues  // code présent mais absent du référentiel, par champ
) {
    // Constructeur court conservé — les 8 autres importeurs compilent sans modification
    public RapportImport(int lus, int crees, int misAJour, List<String> erreurs) {
        this(lus, crees, misAJour, erreurs, Map.of(), Map.of());
    }
}*/
