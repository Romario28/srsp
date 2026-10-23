package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.time.LocalDate;

public record DossierResume(String matricule, TypeAnticipation type, LocalDate dateEcheance,
                            StatutDossier statut, LocalDate dateDemarrage, String motifSansSuite) {
    public static DossierResume de(Dossier d) {
        return new DossierResume(d.getAgent().getMatricule(), d.getType(), d.getDateEcheance(),
                d.getStatut(), d.getDateDemarrage(), d.getMotifSansSuite());
    }
    public CleDossier cle() { return new CleDossier(matricule, type, dateEcheance); }
    public EtatDossier etat(LocalDate aujourd) { return RegleDossier.etat(statut, dateEcheance, aujourd); }
}
