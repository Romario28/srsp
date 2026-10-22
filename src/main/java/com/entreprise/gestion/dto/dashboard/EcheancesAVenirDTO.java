package com.entreprise.gestion.dto.dashboard;

import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;

import java.time.LocalDate;
import java.util.Map;

/** Échéances datées d'agents en activité, sur un intervalle inclusif, hors anomalies. */
public record EcheancesAVenirDTO(int jours, LocalDate du, LocalDate au, long total,
                                 Map<CategorieCalendrier, Long> parCategorie) {}
