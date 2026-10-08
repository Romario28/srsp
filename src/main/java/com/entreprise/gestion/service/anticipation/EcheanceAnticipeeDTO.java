package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.time.LocalDate;

public record EcheanceAnticipeeDTO(
        String matricule,
        String nomComplet,
        TypeAnticipation type,
        LocalDate dateEcheance,
        LocalDate datePreparation,
        GradeSuivant gradeSuivant,
        long joursRestants,
        String details,
        StatutAgent statut,
        LocalDate dateNaissance,      // vérification DEPART_RETRAITE
        LocalDate avanceDate,         //dernier situation
        LocalDate dateDebutContrat,   // vérification TITULARISATION (stagiaire)
        LocalDate dateFinContrat,     // vérification FIN_CONTRAT
        String corpsCode,
        String gradeCode,  //dernier situation
        String categorieCode
) {}
