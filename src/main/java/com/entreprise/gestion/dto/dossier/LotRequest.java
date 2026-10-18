package com.entreprise.gestion.dto.dossier;
import com.entreprise.gestion.entite.anticipation.EtapeDossier;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
public record LotRequest(@NotEmpty @Size(max=500) List<Long> ids,@NotNull EtapeDossier etape,@NotNull LocalDate date,boolean ecraser) {}
