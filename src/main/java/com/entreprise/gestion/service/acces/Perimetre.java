package com.entreprise.gestion.service.acces;

import com.entreprise.gestion.entite.anticipation.Agent;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Ce qu'un compte peut voir. Dans une dimension : le code de l'agent doit figurer dans la liste (OU).
 * Entre dimensions : toutes les conditions s'appliquent (ET). Dimension vide : pas de restriction sur cet axe.
 * Aucune valeur du tout (hors accès total) : aucun agent. Un agent sans la valeur demandée est exclu.
 */
public record Perimetre(boolean accesTotal, Set<String> ministeres, Set<String> soas, Set<String> fivs) {

    /** Remplit un paramètre de requête dont l'axe n'est pas restreint : le drapeau booléen le rend sans effet. */
    private static final String NEUTRE = "-";

    public Perimetre {
        ministeres = Collections.unmodifiableSortedSet(new TreeSet<>(ministeres));
        soas = Collections.unmodifiableSortedSet(new TreeSet<>(soas));
        fivs = Collections.unmodifiableSortedSet(new TreeSet<>(fivs));
    }

    public static Perimetre tout() { return new Perimetre(true, Set.of(), Set.of(), Set.of()); }

    /** aucun agent visible (compte Agent, ou pas de compte authentifié). */
    public static Perimetre aucun() { return new Perimetre(false, Set.of(), Set.of(), Set.of()); }

    public boolean estVide() { return !accesTotal && ministeres.isEmpty() && soas.isEmpty() && fivs.isEmpty(); }

    public boolean autorise(Agent a) {
        if (accesTotal) return true;
        if (estVide()) return false;
        return dans(ministeres, a.getMinistere() == null ? null : a.getMinistere().getCode())
                && dans(soas, a.getSoa() == null ? null : a.getSoa().getCode())
                && dans(fivs, a.getLocalite() == null ? null : a.getLocalite().getCode());
    }

    public boolean restreintMinisteres() { return !accesTotal && !ministeres.isEmpty(); }
    public boolean restreintSoas() { return !accesTotal && !soas.isEmpty(); }
    public boolean restreintFivs() { return !accesTotal && !fivs.isEmpty(); }
    public boolean restreint() { return restreintMinisteres() || restreintSoas() || restreintFivs(); }
    public Collection<String> ministeresPourRequete() { return ministeres.isEmpty() ? List.of(NEUTRE) : ministeres; }
    public Collection<String> soasPourRequete() { return soas.isEmpty() ? List.of(NEUTRE) : soas; }
    public Collection<String> fivsPourRequete() { return fivs.isEmpty() ? List.of(NEUTRE) : fivs; }

    /**
     * Vrai si le périmètre d'un autre compte est entièrement contenu dans celui-ci : sur chaque axe restreint
     * ici, l'autre compte doit être restreint à des valeurs toutes permises. Un compte sans aucune valeur n'est
     * rattaché à personne. Sert à la liste des comptes d'un chef.
     */
    public boolean couvre(Perimetre autre) {
        if (accesTotal) return true;
        if (estVide() || autre.accesTotal() || autre.estVide()) return false;
        return inclus(ministeres, autre.ministeres) && inclus(soas, autre.soas) && inclus(fivs, autre.fivs);
    }

    private static boolean dans(Set<String> permis, String code) {
        return permis.isEmpty() || (code != null && permis.contains(code));
    }

    private static boolean inclus(Set<String> permis, Set<String> demande) {
        return permis.isEmpty() || (!demande.isEmpty() && permis.containsAll(demande));
    }
}
