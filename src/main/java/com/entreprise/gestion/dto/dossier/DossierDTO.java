package com.entreprise.gestion.dto.dossier;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.service.dossier.RegleDossier;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DossierDTO(Long id, String matricule, String nomComplet, StatutAgent statutAgent,
        String corpsCode, String categorieCode, String gradeCode, TypeAnticipation type,
        LocalDate datePreparation, LocalDate dateEcheance, StatutDossier statut,
        LocalDate dateDemarrage, String motifSansSuite, boolean enRetard, Long joursRetard,
        long nbDossiersAgent) {   // total des dossiers de l'agent, tous états confondus

    public static DossierDTO de(Dossier d, LocalDate aujourd, long nbDossiersAgent) {
        Agent a = d.getAgent();
        boolean retard = RegleDossier.enRetard(d, aujourd);
        return new DossierDTO(d.getId(), a.getMatricule(), a.getPrenoms() + " " + a.getNom(), a.getStatut(),
                a.getCorps() == null ? null : a.getCorps().getCode(),
                a.getCorps() == null ? null : a.getCorps().getCategorie(),
                a.getGrade() == null ? null : a.getGrade().getCode(),
                d.getType(), d.getDatePreparation(), d.getDateEcheance(), d.getStatut(),
                d.getDateDemarrage(), d.getMotifSansSuite(),
                retard, retard ? ChronoUnit.DAYS.between(d.getDateEcheance(), aujourd) : null,
                nbDossiersAgent);
    }
}
