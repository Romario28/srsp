package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


@Service
@RequiredArgsConstructor
public class MoteurAnticipation {

    @Value("${anticipation.retraite.age-legal:60}")
    private int ageRetraite;

    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;

    public List<Echeance> calculerEcheances(Agent a) {
        List<Echeance> resultats = new ArrayList<>();
        if (!estEnActivite(a)) return resultats;

        ajouter(resultats, calculerRetraite(a));
        ajouter(resultats, calculerAvancementOuAnomalie(a));
        ajouter(resultats, calculerFinContrat(a));
        return resultats;
    }

    private boolean estEnActivite(Agent a) {
        return a.getSanction() == null
                || !CodesSituationAdministrative.SORTIE_DEFINITIVE.contains(a.getSanction().getCode());
    }

    private Echeance calculerRetraite(Agent a) {
        if (a.getDateNaissance() == null) return anomalie(a, "Date de naissance manquante");
        LocalDate echeance = a.getDateNaissance().plusYears(ageRetraite);
        return new Echeance(a.getMatricule(), nomComplet(a), TypeAnticipation.DEPART_RETRAITE, echeance, null);
    }


    private Echeance calculerAvancementOuAnomalie(Agent a) {
        if (a.getGrade() == null)  return anomalie(a, "Grade manquant");
        if (a.getCorps() == null)  return anomalie(a, "Corps manquant");

        boolean estEld       = a.getStatut() == StatutAgent.ELD;
        boolean estStagiaire = a.getGrade().getCode().startsWith("ST");

        if (estEld)       return calculerPalierEld(a);
        if (estStagiaire) return calculerTitularisation(a);
        return calculerAvancementStandard(a);
    }

    private Echeance calculerPalierEld(Agent a) {
        if (a.getAvanceDate() == null) return anomalie(a, "Date du dernier palier manquante (ELD)");
        LocalDate echeance = a.getAvanceDate().plusYears(2);
        return new Echeance(a.getMatricule(), nomComplet(a), TypeAnticipation.AVANCEMENT,
                echeance, "Palier suivant (ELD, +2 ans fixe)");
    }

    private Echeance calculerTitularisation(Agent a) {
        LocalDate dateAncrage = a.getAvanceDate() != null ? a.getAvanceDate() : a.getDateDebutContrat();
        if (dateAncrage == null) {
            return anomalie(a, "Ni date d'avancement ni date de début de contrat renseignée (stagiaire)");
        }
        Integer duree = dureeRequise(a);
        if (duree == null) {
            return anomalie(a, "Durée de stage non renseignée pour "
                    + a.getCorps().getCode() + "/" + a.getGrade().getCode());
        }
        LocalDate echeance = dateAncrage.plusYears(duree);
        return new Echeance(a.getMatricule(), nomComplet(a), TypeAnticipation.TITULARISATION, echeance, null);
    }


    private Echeance calculerAvancementStandard(Agent a) {
        if (a.getAvanceDate() == null) return anomalie(a, "Date du dernier avancement manquante");
        Integer duree = dureeRequise(a);
        if (duree == null) return anomalie(a, "Durée requise non renseignée pour "
                + a.getCorps().getCode() + "/" + a.getGrade().getCode());
        LocalDate echeance = a.getAvanceDate().plusYears(duree);
        return new Echeance(a.getMatricule(), nomComplet(a), TypeAnticipation.AVANCEMENT, echeance, null);
    }


    private Echeance calculerFinContrat(Agent a) {
        // Absence de date_fin_contrat = normal pour contrat indeterminé
        if (a.getDateFinContrat() == null) return null;
        return new Echeance(a.getMatricule(), nomComplet(a), TypeAnticipation.FIN_CONTRAT,
                a.getDateFinContrat(), null);
    }

//    private Integer dureeRequise(String corpsCode, String gradeCode) {
//        return indiceGrdCorpsRepository.findById(new IndiceGrdCorpsId(gradeCode, corpsCode))
//                .map(IndiceGrdCorps::getDureeRequise)
//                .orElse(null);
//    }

    private Integer dureeRequise(Agent a) {
        if (a.getGrade() == null || a.getCorps() == null) {
            return null;
        }
        return indiceGrdCorpsRepository
                .findById(new IndiceGrdCorpsId(
                        a.getGrade().getCode(),
                        new CorpsId(a.getCorps().getCode(), a.getCorps().getCategorie())))
                .map(IndiceGrdCorps::getDureeRequise)
                .orElse(null);
    }

    private Echeance anomalie(Agent a, String message) {
        return new Echeance(a.getMatricule(), nomComplet(a), TypeAnticipation.ANOMALIE, null, message);
    }

    private String nomComplet(Agent a) { return a.getPrenoms() + " " + a.getNom(); }

    private void ajouter(List<Echeance> liste, Echeance e) { if (e != null) liste.add(e); }
}