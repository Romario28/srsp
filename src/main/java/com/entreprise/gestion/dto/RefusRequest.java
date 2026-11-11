package com.entreprise.gestion.dto;

import jakarta.validation.constraints.Size;

public record RefusRequest(@Size(max = 300) String motif) {}
