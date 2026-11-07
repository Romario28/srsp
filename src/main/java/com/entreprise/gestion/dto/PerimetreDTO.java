package com.entreprise.gestion.dto;

import java.util.List;

/** Périmètre d'un compte (entrée et sortie). Une liste vide = pas de restriction sur cet axe. */
public record PerimetreDTO(List<String> ministeres, List<String> soas, List<String> fivs) {}
