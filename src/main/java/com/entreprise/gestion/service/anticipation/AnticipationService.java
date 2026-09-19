package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
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
    public List<AlerteAnticipation> departsRetraite(Integer horizonJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.DEPART_RETRAITE, horizonJours, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> avancementsDus(Integer horizonJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.AVANCEMENT, horizonJours, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> titularisationsDues(Integer horizonJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.TITULARISATION, horizonJours, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> finsContrat(Integer horizonJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.FIN_CONTRAT, horizonJours, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> anomalies(StatutAgent statut) {
        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> versAlerte(a, e)))
                .collect(Collectors.toList());
    }
    /** horizonJoursDemande == null → on utilise le délai configuré (ou son défaut) pour ce type. */
    private List<AlerteAnticipation> calculerParType(TypeAnticipation type, Integer horizonJoursDemande, StatutAgent statut) {
        int horizon = horizonJoursDemande != null
                ? horizonJoursDemande
                : configurationDelaiService.resoudreDelai(type);

        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
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
        if (e.type() != TypeAnticipation.ANOMALIE && jours < 0) {
            String retard = "Dépassé de " + formatRetard(-jours);
            details = (details != null) ? details + " — " + retard : retard;
        }

        return new AlerteAnticipation(
                a.getMatricule(),
                a.getPrenoms() + " " + a.getNom(),
                e.type(),
                e.dateEcheance(),
                jours,
                details,
                a.getStatut(),
                a.getDateNaissance(),
                a.getAvanceDate(),
                a.getDateDebutContrat(),
                a.getDateFinContrat(),
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

