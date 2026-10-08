package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.Alerte;
import com.entreprise.gestion.entite.anticipation.StatutAlerte;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.GradeSuivant;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Projection API d'une alerte — n'embarque jamais l'entité {@code Utilisateur}
 * (relations bidirectionnelles → risque de boucle JSON / 500 à l'acquittement).
 */
@Data
@Builder
public class AlerteDTO {

    private Long id;
    private String matriculeAgent;
    private String nomCompletAgent;
    private TypeAnticipation type;
    private LocalDate dateEcheance;
    private LocalDate datePreparation;
    private GradeSuivant gradeSuivant;
    private String gradeActuel;
    private LocalDate dateEffetActuelle;
    private String details;
    private StatutAlerte statut;
    private LocalDateTime dateDetection;
    private LocalDateTime dateDerniereConsultation;
    private LocalDateTime dateAcquittement;
    /** Email de l'utilisateur ayant acquitté, ou null si non acquittée. */
    private String acquitteeParEmail;

    public static AlerteDTO from(Alerte a) {
        return AlerteDTO.builder()
                .id(a.getId())
                .matriculeAgent(a.getMatriculeAgent())
                .nomCompletAgent(a.getNomCompletAgent())
                .type(a.getType())
                .dateEcheance(a.getDateEcheance())
                .datePreparation(a.getDatePreparation())
                .gradeSuivant(GradeSuivant.depuisStockage(a.getGradeSuivantCas(), a.getGradeSuivant()))
                .gradeActuel(a.getGradeActuel())
                .dateEffetActuelle(a.getDateEffetActuelle())
                .details(a.getDetails())
                .statut(a.getStatut())
                .dateDetection(a.getDateDetection())
                .dateDerniereConsultation(a.getDateDerniereConsultation())
                .dateAcquittement(a.getDateAcquittement())
                .acquitteeParEmail(a.getAcquitteePar() != null ? a.getAcquitteePar().getEmail() : null)
                .build();


    }
}
