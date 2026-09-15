package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnticipationService {

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final ConfigurationDelaiService configurationDelaiService;

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> departsRetraite(Integer horizonJours) {
        return calculerParType(TypeAnticipation.DEPART_RETRAITE, horizonJours);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> avancementsDus(Integer horizonJours) {
        return calculerParType(TypeAnticipation.AVANCEMENT, horizonJours);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> titularisationsDues(Integer horizonJours) {
        return calculerParType(TypeAnticipation.TITULARISATION, horizonJours);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> finsContrat(Integer horizonJours) {
        return calculerParType(TypeAnticipation.FIN_CONTRAT, horizonJours);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> anomalies() {
        return agentRepository.findAllActifs(CodesSituationAdministrative.SORTIE_DEFINITIVE).stream()
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> versAlerte(a, e)))
                .collect(Collectors.toList());
    }

    /** horizonJoursDemande == null → on utilise le délai configuré (ou son défaut) pour ce type. */
    private List<AlerteAnticipation> calculerParType(TypeAnticipation type, Integer horizonJoursDemande) {
        int horizon = horizonJoursDemande != null
                ? horizonJoursDemande
                : configurationDelaiService.resoudreDelai(type);

        return agentRepository.findAllActifs(CodesSituationAdministrative.SORTIE_DEFINITIVE).stream()
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == type)
                        .map(e -> versAlerte(a, e)))
                .filter(al -> al.joursRestants() <= horizon)
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    private AlerteAnticipation versAlerte(Agent a, Echeance e) {
        long jours = e.dateEcheance() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), e.dateEcheance())
                : 0;

        String details = e.details();
        if (e.type() != TypeAnticipation.ANOMALIE && jours < 0 && details == null) {
            details = "Dépassé de " + formatRetard(-jours);
        }

        return new AlerteAnticipation(
                a.getMatricule(), a.getPrenoms() + " " + a.getNom(), e.type(), e.dateEcheance(),
                jours, details, a.getDateNaissance(), a.getAvanceDate(), a.getDateDebutContrat(),
                a.getCorps() != null ? a.getCorps().getCode() : null,
                a.getGrade() != null ? a.getGrade().getCode() : null,
                a.getCorps() != null ? a.getCorps().getCategorie() : null
        );
    }

    private String formatRetard(long jours) {
        long mois = jours / 30;
        return mois < 12 ? mois + " mois" : (mois / 12) + " an(s) et " + (mois % 12) + " mois";
    }
}

/*
package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnticipationService {

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;

    *//** horizonJours ~ 548 = 18 mois. Inclut aussi les retards (jours négatifs). *//*
    @Transactional(readOnly = true)
    public List<AlerteAnticipation> departsRetraite(int horizonJours) {
        return calculerParType(TypeAnticipation.DEPART_RETRAITE, horizonJours);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> avancementsDus(int horizonJours) {
        return calculerParType(TypeAnticipation.AVANCEMENT, horizonJours);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> titularisationsDues(int horizonJours) {
        return calculerParType(TypeAnticipation.TITULARISATION, horizonJours);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> finsContrat(int horizonJours) {
        return calculerParType(TypeAnticipation.FIN_CONTRAT, horizonJours);
    }

    *//** Séparé des autres : pas de notion d'horizon, une anomalie doit toujours remonter. *//*
    @Transactional(readOnly = true)
    public List<AlerteAnticipation> anomalies() {
        return agentRepository.findAllActifs(CodesSituationAdministrative.SORTIE_DEFINITIVE).stream()
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> versAlerte(a, e)))
                .collect(Collectors.toList());
    }

    private List<AlerteAnticipation> calculerParType(TypeAnticipation type, int horizonJours) {
        return agentRepository.findAllActifs(CodesSituationAdministrative.SORTIE_DEFINITIVE).stream()
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == type)
                        .map(e -> versAlerte(a, e)))
                .filter(al -> al.joursRestants() <= horizonJours)
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    private AlerteAnticipation versAlerte(Agent a, Echeance e) {
        long jours = e.dateEcheance() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), e.dateEcheance())
                : 0;

        String details = e.details();
        if (e.type() != TypeAnticipation.ANOMALIE && jours < 0 && details == null) {
            details = "Dépassé de " + formatRetard(-jours);
        }

        return new AlerteAnticipation(
                a.getMatricule(),
                a.getPrenoms() + " " + a.getNom(),
                e.type(),
                e.dateEcheance(),
                jours,
                details,
                a.getDateNaissance(),
                a.getAvanceDate(),
                a.getDateDebutContrat(),
                a.getCorps() != null ? a.getCorps().getCode() : null,
                a.getGrade() != null ? a.getGrade().getCode() : null,
                a.getCorps() != null ? a.getCorps().getCategorie() : null
        );
    }

    private String formatRetard(long jours) {
        long mois = jours / 30;
        return mois < 12 ? mois + " mois" : (mois / 12) + " an(s) et " + (mois % 12) + " mois";
    }
}*/
/*
// AnticipationService.java
package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnticipationService {

    private final AgentRepository agentRepository;
    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> departsRetraite(int seuilAnnees) {
        LocalDate limite = LocalDate.now().minusYears(seuilAnnees);
        return agentRepository.findByStatutAndDateNaissanceLessThanEqual(StatutAgent.FONCTIONNAIRE, limite)
                .stream()
                .filter(this::estEnActivite)
                .map(a -> alerteRetraite(a, seuilAnnees))
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> avancementsDus(int horizonJours) {
        return agentRepository.findAllWithCorpsGradeForAvancement().stream()
                .filter(this::estEnActivite)
                .map(this::alerteAvancement)
                .filter(Objects::nonNull)
                .filter(al -> al.joursRestants() <= horizonJours)
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    private boolean estEnActivite(Agent a) {
        return a.getSanction() == null
                || !CodesSituationAdministrative.SORTIE_DEFINITIVE.contains(a.getSanction().getCode());
    }

    private AlerteAnticipation alerteRetraite(Agent a, int seuil) {
        LocalDate echeance = a.getDateNaissance().plusYears(seuil);
        long jours = ChronoUnit.DAYS.between(LocalDate.now(), echeance);
        return new AlerteAnticipation(a.getMatricule(), a.getPrenoms() + " " + a.getNom(),
                TypeAnticipation.DEPART_RETRAITE, echeance, jours,
                jours < 0 ? "Dépassé de " + Math.abs(jours) / 365 + " an(s)" : null);
    }

    private AlerteAnticipation alerteAvancement(Agent a) {
        if (a.getAvanceDate() == null || a.getCorps() == null || a.getGrade() == null) return null;

        IndiceGrdCorps ref = indiceGrdCorpsRepository
                .findById(new IndiceGrdCorpsId(
                        a.getGrade().getCode(),
                        new CorpsId(a.getCorps().getCode(), a.getCorps().getCategorie())))
                .orElse(null);
        if (ref == null || ref.getDureeRequise() == null) return null;

        LocalDate echeance = a.getAvanceDate().plusMonths(ref.getDureeRequise());
        long jours = ChronoUnit.DAYS.between(LocalDate.now(), echeance);
        return new AlerteAnticipation(a.getMatricule(), a.getPrenoms() + " " + a.getNom(),
                TypeAnticipation.AVANCEMENT, echeance, jours, null);
    }
}*/
