package com.entreprise.gestion.service.exports;

import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.entite.anticipation.StatutAlerte;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.CritereDate;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import java.time.LocalDate;
import java.util.Set;

public record DemandeExport(Ecran ecran, FiltreAgents filtre, Integer prevenanceMois, Integer retardMois,
        LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate, Set<CategorieCalendrier> typesCalendrier,
        TypeAnticipation type, StatutAlerte statutAlerte, String matricule) {}
