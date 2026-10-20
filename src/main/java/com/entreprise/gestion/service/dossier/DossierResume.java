package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.EtapeDossier;
import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.time.LocalDate;

public record DossierResume(String matricule,TypeAnticipation type,LocalDate dateEcheance,StatutDossier statut,
        LocalDate chefAvantFop,LocalDate fop,LocalDate visa,LocalDate cf,LocalDate prefet,LocalDate persTana,
        LocalDate solde,LocalDate fin,String motifSansSuite) {
    public static DossierResume de(Dossier d) {
        return new DossierResume(d.getAgent().getMatricule(), d.getType(), d.getDateEcheance(), d.getStatut(),
                d.getDateChefAvantFop(), d.getDateFop(), d.getDateVisa(), d.getDateCf(), d.getDatePrefet(),
                d.getDatePersTana(), d.getDateSolde(), d.getDateFin(), d.getMotifSansSuite());
    }
    public CleDossier cle(){return new CleDossier(matricule,type,dateEcheance);}
    public EtatDossier etat(LocalDate aujourd){return RegleDossier.etat(statut,dateEcheance,aujourd);}
    public LocalDate etape(EtapeDossier etape){return switch(etape){
        case CHEF_AVANT_FOP->chefAvantFop;case FOP->fop;case VISA->visa;case CF->cf;
        case PREFET->prefet;case PERS_TANA->persTana;case SOLDE->solde;case FIN->fin;
    };}
}
