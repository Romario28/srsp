package com.entreprise.gestion.dto.dossier;

import java.time.LocalDate;

/** Un compteur par onglet. aRelancer est un sous-ensemble de enCours (badge, pas un onglet). */
public record SyntheseDossiersDTO(long aTraiter, long enRetard, long enCours, long traites, long sansSuite,
                                  long aRelancer, LocalDate dateReprise, int relanceApresJours) {}