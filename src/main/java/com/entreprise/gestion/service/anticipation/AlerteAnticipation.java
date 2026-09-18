package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.time.LocalDate;

public record AlerteAnticipation(
        String matricule,
        String nomComplet,
        TypeAnticipation type,
        LocalDate dateEcheance,
        long joursRestants,
        String details,
        StatutAgent statut,
        LocalDate dateNaissance,      // vérification DEPART_RETRAITE
        LocalDate avanceDate,         // vérification AVANCEMENT (standard/ELD)
        LocalDate dateDebutContrat,   // vérification TITULARISATION (stagiaire)
        LocalDate dateFinContrat,     // vérification FIN_CONTRAT
        String corpsCode,
        String gradeCode,
        String categorieCode
) {}