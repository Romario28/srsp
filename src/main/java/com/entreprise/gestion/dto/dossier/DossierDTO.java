package com.entreprise.gestion.dto.dossier;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.service.dossier.RegleDossier;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.Map;

public record DossierDTO(Long id,Long version,String matricule,String nomComplet,StatutAgent statutAgent,
        String corpsCode,String categorieCode,String gradeCode,TypeAnticipation type,LocalDate datePreparation,
        LocalDate dateEcheance,Map<EtapeDossier,LocalDate> etapes,StatutDossier statut,String motifSansSuite,
        boolean enRetard,Long joursRetard,boolean aRelancer,Long joursSansNouvelle) {
    public static DossierDTO de(Dossier d,LocalDate aujourd,int relanceApresJours) {
        Agent a=d.getAgent(); Map<EtapeDossier,LocalDate> etapes=new EnumMap<>(EtapeDossier.class);
        for(EtapeDossier e:EtapeDossier.values())etapes.put(e,e.de(d));
        boolean retard=RegleDossier.enRetard(d,aujourd), relance=RegleDossier.aRelancer(d,aujourd,relanceApresJours);
        LocalDate derniere=RegleDossier.derniereEtape(d);
        return new DossierDTO(d.getId(),d.getVersion(),a.getMatricule(),a.getPrenoms()+" "+a.getNom(),a.getStatut(),
                a.getCorps()==null?null:a.getCorps().getCode(),a.getCorps()==null?null:a.getCorps().getCategorie(),
                a.getGrade()==null?null:a.getGrade().getCode(),d.getType(),d.getDatePreparation(),d.getDateEcheance(),etapes,d.getStatut(),d.getMotifSansSuite(),
                retard,retard?ChronoUnit.DAYS.between(d.getDateEcheance(),aujourd):null,relance,
                relance&&derniere!=null?ChronoUnit.DAYS.between(derniere,aujourd):null);
    }
}
