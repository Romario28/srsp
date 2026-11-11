package com.entreprise.gestion.dto;

/** Périmètre accordé à l'approbation (profils RH et Chef seulement). Absent ou vide : aucune valeur. */
public record ApprobationRequest(PerimetreDTO perimetre) {}
