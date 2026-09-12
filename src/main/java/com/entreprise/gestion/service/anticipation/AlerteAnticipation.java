// AlerteAnticipation.java
package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;

import java.time.LocalDate;

public record AlerteAnticipation(
        String matricule,
        String nomComplet,
        TypeAnticipation type,
        LocalDate dateEcheance,
        long joursRestants,
        String details
) {}