package com.entreprise.gestion.dto.anticipation;

import java.util.List;

/** Valeurs réellement présentes chez les agents en activité. Les statuts sont fixes : l'énumération suffit. */
public record OptionsFiltresDTO(
        List<OptionFiltre> corps, List<OptionFiltre> grades, List<OptionFiltre> categories,
        List<OptionFiltre> fiv, List<OptionFiltre> soa, List<OptionFiltre> regions,
        List<OptionFiltre> ministeres) {}
