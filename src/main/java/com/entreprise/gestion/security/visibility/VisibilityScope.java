package com.entreprise.gestion.security.visibility;

import lombok.Getter;

import java.util.Collections;
import java.util.Set;

/**
 * accesTotal = true (ADMIN) → aucun filtre, lecture ET écriture partout.
 *
 * Sinon, DEUX ensembles de chemins distincts, reflétant TypeAcces :
 *  - cheminsLecture   : chef de département (implique lecture+écriture,
 *                       donc aussi ajouté ici) + délégations LECTURE ou
 *                       LECTURE_ECRITURE actives.
 *  - cheminsEcriture  : chef de département + délégations LECTURE_ECRITURE
 *                       actives uniquement. Sous-ensemble de cheminsLecture.
 *
 * Jamais construit à partir d'un paramètre client — toujours recalculé
 * côté serveur (VisibiliteService.resoudreScope).
 */
@Getter
public final class VisibilityScope {

    private final boolean accesTotal;
    private final Set<String> cheminsLecture;
    private final Set<String> cheminsEcriture;
    private final Set<Long>   employeIdsAutorises;

    private VisibilityScope(boolean accesTotal, Set<String> cheminsLecture,
                            Set<String> cheminsEcriture, Set<Long> employeIds) {
        this.accesTotal = accesTotal;
        this.cheminsLecture = cheminsLecture;
        this.cheminsEcriture = cheminsEcriture;
        this.employeIdsAutorises = employeIds;
    }

    public static VisibilityScope tout() {
        return new VisibilityScope(true, Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
    }

    public static VisibilityScope restreint(Set<String> cheminsLecture, Set<String> cheminsEcriture,
                                            Set<Long> employeIds) {
        return new VisibilityScope(false, Set.copyOf(cheminsLecture), Set.copyOf(cheminsEcriture), Set.copyOf(employeIds));
    }

    public boolean estVide() {
        return !accesTotal && cheminsLecture.isEmpty() && employeIdsAutorises.isEmpty();
    }

    public boolean autoriseLecture(Long employeId, String employeChemin) {
        if (accesTotal) return true;
        if (employeId != null && employeIdsAutorises.contains(employeId)) return true;
        if (employeChemin == null) return false;
        return cheminsLecture.stream().anyMatch(employeChemin::startsWith);
    }

    /** employeId peut être null (cas création : l'employé n'existe pas encore). */
    public boolean autoriseEcriture(Long employeId, String chemin) {
        if (accesTotal) return true;
        if (employeId != null && employeIdsAutorises.contains(employeId)) return true;
        if (chemin == null) return false;
        return cheminsEcriture.stream().anyMatch(chemin::startsWith);
    }
}