package com.entreprise.gestion.dto.dossier;

import java.time.LocalDate;

/** Un compteur par onglet (hors « Tous »). */
public record SyntheseDossiersDTO(long aTraiter, long enRetard, long enCours, long sansSuite, LocalDate dateReprise) {}
