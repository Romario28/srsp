package com.entreprise.gestion.dto.anticipation;

/** Une valeur proposée dans une liste de filtre. Top-level : HQL ne sait pas instancier une classe imbriquée. */
public record OptionFiltre(String code, String libelle) {}
