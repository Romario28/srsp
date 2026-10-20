package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.dossier.EtatDossier;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

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
        String categorieCode,
        EtatDossier dossier
) {
    public static EcheanceAnticipeeDTO de(Agent a,Echeance e,FenetreAnticipation fenetre,GradesParCorps grades) {
        return de(a,e,fenetre,grades,null);
    }

    public static EcheanceAnticipeeDTO de(Agent a,Echeance e,FenetreAnticipation fenetre,GradesParCorps grades,EtatDossier dossier) {
        long jours = e.dateEcheance() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), e.dateEcheance())
                : 0;

        return new EcheanceAnticipeeDTO(
                a.getMatricule(),
                a.getPrenoms() + " " + a.getNom(),
                e.type(),
                e.dateEcheance(),
                fenetre != null ? fenetre.datePreparation(e.dateEcheance()) : null,
                grades != null && a.getCorps() != null && a.getGrade() != null
                        ? grades.pour(a.getCorps().getCode(), a.getCorps().getCategorie(), a.getGrade().getCode())
                        : (grades != null ? GradeSuivant.INDETERMINE : null),
                jours,
                e.details(),
                a.getStatut(),
                a.getDateNaissance(),
                a.getAvanceDate(),
                a.getDateDebutContrat(),
                a.getDateFinContrat(),
                a.getCorps() != null ? a.getCorps().getCode() : null,
                a.getGrade() != null ? a.getGrade().getCode() : null,
                a.getCorps() != null ? a.getCorps().getCategorie() : null,
                dossier
        );
    }
}
