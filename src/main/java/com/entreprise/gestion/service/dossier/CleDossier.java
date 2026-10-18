package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.time.LocalDate;

public record CleDossier(String matricule,TypeAnticipation type,LocalDate dateEcheance) {}
