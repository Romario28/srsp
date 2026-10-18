package com.entreprise.gestion.dto.dossier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record SansSuiteRequest(@NotBlank @Size(max=500) String motif) {}
