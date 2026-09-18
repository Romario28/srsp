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
import java.util.Set;


@Service
@RequiredArgsConstructor
public class MoteurAnticipation {

    @Value("${anticipation.retraite.age-legal:60}")
    private int ageRetraite;

    @Value("${anticipation.grades-stagiaire:ST0E}")
    private Set<String> gradesStagiaire;

    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;

    public List<Echeance> calculerEcheances(Agent a) {
        List<Echeance> resultats = new ArrayList<>();

        ajouter(resultats, calculerRetraite(a));
        ajouter(resultats, calculerAvancementOuAnomalie(a));
        ajouter(resultats, calculerFinContrat(a));
        return resultats;
    }

    private Echeance calculerRetraite(Agent a) {
        if (a.getDateNaissance() == null) return anomalie(a, "Date de naissance manquante");
        LocalDate echeance = a.getDateNaissance().plusYears(ageRetraite);
        return new Echeance(a.getMatricule(), nomComplet(a), TypeAnticipation.DEPART_RETRAITE, echeance, null);
    }




    // Dispatcher : préconditions + classification (TITULARISATION vs AVANCEMENT)
    private Echeance calculerAvancementOuAnomalie(Agent a) {
        boolean gradeManquant = a.getGrade() == null;
        boolean corpsManquant = a.getCorps() == null;

        if (gradeManquant && corpsManquant) return anomalie(a, "Grade et corps manquants");
        if (gradeManquant) return anomalie(a, "Grade manquant");
        if (corpsManquant) return anomalie(a, "Corps manquant");

        TypeAnticipation type = gradesStagiaire.contains(a.getGrade().getCode())
                ? TypeAnticipation.TITULARISATION
                : TypeAnticipation.AVANCEMENT;

        return calculerAvancement(a, type);
    }

    /**
     * Calcul pur : suppose grade et corps déjà vérifiés non-null par l'appelant
     * (calculerAvancementOuAnomalie) — ne les revérifie pas.
     * échéance = (avance_date ?: date_debut_contrat) + durée_requise(corps, grade),
     * identique pour tous les statuts (FONCTIONNAIRE, CONTRACTUEL, ELD).
     */
    private Echeance calculerAvancement(Agent a, TypeAnticipation type) {
        boolean ancrageParContrat = (a.getAvanceDate() == null);
        LocalDate ancrage = ancrageParContrat ? a.getDateDebutContrat() : a.getAvanceDate();
        if (ancrage == null) {
            return anomalie(a, "Ni date d'avancement ni date de début de contrat renseignée");
        }

        Integer duree = dureeRequise(a);
        if (duree == null) {
            return anomalie(a, "Durée requise non renseignée pour "
                    + a.getCorps().getCode() + "/" + a.getCorps().getCategorie()
                    + "/" + a.getGrade().getCode());
        }

        String details = ancrageParContrat ? "Ancrage : date de début de contrat (avance_date absente)" : null;

        return new Echeance(a.getMatricule(), nomComplet(a), type, ancrage.plusYears(duree), details);
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