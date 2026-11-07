package com.entreprise.gestion.service.acces;

import com.entreprise.gestion.entite.anticipation.Agent;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ce qu'un compte peut voir. Dans une dimension : le code de l'agent doit figurer dans la liste (OU).
 * Entre dimensions : toutes les conditions s'appliquent (ET). Dimension vide : pas de restriction sur cet axe.
 * Aucune valeur du tout (hors accès total) : aucun agent. Un agent sans la valeur demandée est exclu.
 */
public record Perimetre(boolean accesTotal, Set<String> ministeres, Set<String> soas, Set<String> fivs) {

    public Perimetre {
        ministeres = Collections.unmodifiableSortedSet(new TreeSet<>(ministeres));
        soas = Collections.unmodifiableSortedSet(new TreeSet<>(soas));
        fivs = Collections.unmodifiableSortedSet(new TreeSet<>(fivs));
    }

    public static Perimetre tout() { return new Perimetre(true, Set.of(), Set.of(), Set.of()); }

    public boolean estVide() { return !accesTotal && ministeres.isEmpty() && soas.isEmpty() && fivs.isEmpty(); }

    public boolean autorise(Agent a) {
        if (accesTotal) return true;
        if (estVide()) return false;
        return dans(ministeres, a.getMinistere() == null ? null : a.getMinistere().getCode())
                && dans(soas, a.getSoa() == null ? null : a.getSoa().getCode())
                && dans(fivs, a.getLocalite() == null ? null : a.getLocalite().getCode());
    }

    private static boolean dans(Set<String> permis, String code) {
        return permis.isEmpty() || (code != null && permis.contains(code));
    }
}
