package com.entreprise.gestion.dto.dashboard;

import java.time.LocalDate;
import java.util.List;

/** Effectif des agents en activité, répartition par âge et projection des départs à la retraite. */
public record EffectifsDTO(long total, long fonctionnaires, long contractuels, long eld, long statutNonRenseigne,
                           Ages ages, Retraites retraites) {
    public record Bornes(int min, int pas, int max) {}
    public record LigneAge(int tranche, String sexe, String categorie, long nombre) {}
    public record Ages(Bornes bornes, int ageLegal, List<LigneAge> lignes, long sansDateNaissance) {}
    public record Periode(String cle, long nombre) {}
    public record Retraites(LocalDate depuis, List<Periode> mois, List<Periode> annees, long dejaAtteint) {}
}
