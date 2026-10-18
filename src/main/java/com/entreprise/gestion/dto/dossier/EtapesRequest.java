package com.entreprise.gestion.dto.dossier;
import com.entreprise.gestion.entite.anticipation.EtapeDossier;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.Map;
public record EtapesRequest(@NotNull Long version,@NotNull Map<EtapeDossier,LocalDate> etapes) {}
