package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.EtapeDossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.exception.BusinessException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RegleDossier {
    private static final DateTimeFormatter FMT=DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static StatutDossier statutSelonEtapes(Dossier d) {
        if(d.getDateFin()!=null)return StatutDossier.TRAITE;
        if(d.getDateChefAvantFop()!=null)return StatutDossier.EN_COURS;
        return StatutDossier.EN_ATTENTE_DE_TRAITEMENT;
    }
    public static boolean enRetard(Dossier d,LocalDate aujourd) {
        return d.getStatut()==StatutDossier.EN_ATTENTE_DE_TRAITEMENT && d.getDatePreparation().isBefore(aujourd);
    }
    public static LocalDate derniereEtape(Dossier d) {
        LocalDate last=null;
        for(EtapeDossier e:EtapeDossier.values()){LocalDate v=e.de(d);if(v!=null&&(last==null||v.isAfter(last)))last=v;}
        return last;
    }
    public static boolean aRelancer(Dossier d,LocalDate aujourd,int jours) {
        if(d.getStatut()!=StatutDossier.EN_COURS)return false;
        LocalDate last=derniereEtape(d);return last!=null&&last.isBefore(aujourd.minusDays(jours));
    }
    public static void verifier(Dossier d,LocalDate aujourd) {
        boolean demarre=d.getDateChefAvantFop()!=null; EtapeDossier precedente=null; LocalDate datePrecedente=null;
        for(EtapeDossier etape:EtapeDossier.values()) {
            LocalDate v=etape.de(d);if(v==null)continue;
            if(v.isAfter(aujourd))throw new BusinessException("DATE_FUTURE","« "+etape.libelle()+" » : la date "+fmt(v)+" est dans le futur.");
            if(etape!=EtapeDossier.CHEF_AVANT_FOP&&!demarre)throw new BusinessException("ETAPE_SANS_DEPART","Renseignez d'abord « Chef avant FOP » : le dossier doit être démarré avant les autres étapes.");
            if(datePrecedente!=null&&v.isBefore(datePrecedente))throw new BusinessException("ORDRE_ETAPES","« "+etape.libelle()+" » ("+fmt(v)+") ne peut pas précéder « "+precedente.libelle()+" » ("+fmt(datePrecedente)+").");
            precedente=etape;datePrecedente=v;
        }
    }
    public static String fmt(LocalDate d){return d==null?"—":d.format(FMT);}
    private RegleDossier(){}
}
