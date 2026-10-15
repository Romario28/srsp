package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;

/**
 * Filtres cumulables (ET) sur les agents. null ou vide = pas de filtre sur ce critère.
 * Un agent dont l'attribut est absent ne correspond à aucun filtre posé sur cet attribut.
 * Lié aux paramètres de requête par leur nom (@ModelAttribute).
 */
public record FiltreAgents(StatutAgent statutAgent, String corps, String grade, String categorie,
                           String fiv, String soa, String region, String ministere) {

    public static final FiltreAgents AUCUN = new FiltreAgents(null, null, null, null, null, null, null, null);

    public FiltreAgents {
        corps = nettoyer(corps);
        grade = nettoyer(grade);
        categorie = nettoyer(categorie);
        fiv = nettoyer(fiv);
        soa = nettoyer(soa);
        region = nettoyer(region);
        ministere = nettoyer(ministere);
    }

    public boolean estVide() {
        return statutAgent == null && corps == null && grade == null && categorie == null
                && fiv == null && soa == null && region == null && ministere == null;
    }

    /** Même lecture que l'import : codes comparés tels quels, casse comprise. */
    public boolean accepte(Agent a) {
        return (statutAgent == null || a.getStatut() == statutAgent)
                && (corps == null || corps.equals(a.getCorps() != null ? a.getCorps().getCode() : null))
                && (categorie == null || categorie.equals(a.getCorps() != null ? a.getCorps().getCategorie() : null))
                && (grade == null || grade.equals(a.getGrade() != null ? a.getGrade().getCode() : null))
                && (fiv == null || fiv.equals(a.getLocalite() != null ? a.getLocalite().getCode() : null))
                && (soa == null || soa.equals(a.getSoa() != null ? a.getSoa().getCode() : null))
                && (region == null || region.equals(a.getRegCode()))
                && (ministere == null || ministere.equals(a.getMinistere() != null ? a.getMinistere().getCode() : null));
    }

    private static String nettoyer(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }
}
