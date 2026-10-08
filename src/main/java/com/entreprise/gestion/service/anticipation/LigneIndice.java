package com.entreprise.gestion.service.anticipation;

/** Projection minimale d'une ligne indice grade × corps. */
public record LigneIndice(String corpsCode, String categorie, String gradeCode, String indice) {}
