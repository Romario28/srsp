package com.entreprise.gestion.dto.dossier;
import java.time.LocalDate;
public record SyntheseDossiersDTO(long enAttente,long enCours,long enRetard,long aRelancer,long termines,LocalDate dateReprise,int relanceApresJours) {}
