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

    // MODIFIÉ — "Integer horizonJours" → "Integer prevenanceJours, Integer retardJours"
    // (StatutAgent statut : paramètre déjà existant, conservé à l'identique)
    @Transactional(readOnly = true)
    public List<AlerteAnticipation> departsRetraite(Integer prevenanceJours, Integer retardJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.DEPART_RETRAITE, prevenanceJours, retardJours, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> avancementsDus(Integer prevenanceJours, Integer retardJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.AVANCEMENT, prevenanceJours, retardJours, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> titularisationsDues(Integer prevenanceJours, Integer retardJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.TITULARISATION, prevenanceJours, retardJours, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> finsContrat(Integer prevenanceJours, Integer retardJours, StatutAgent statut) {
        return calculerParType(TypeAnticipation.FIN_CONTRAT, prevenanceJours, retardJours, statut);
    }

    // anomalies() — inchangée (pas de fenêtre pour ce type)
    @Transactional(readOnly = true)
    public List<AlerteAnticipation> anomalies(StatutAgent statut) {
        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> versAlerte(a, e)))
                .collect(Collectors.toList());
    }

    /**
     * prevenanceJours/retardJours null → fenêtre résolue depuis la configuration
     * (DB active sinon défaut) ; non-null → surcharge ponctuelle de cet appel,
     * sans toucher à la configuration persistée.
     */
    // MODIFIÉ — remplace "int horizon = ...; filter(al.joursRestants() <= horizon)"
    // par la résolution de FenetreAnticipation + fenetre.contient(...).
    // Le filtre "statut == null || a.getStatut() == statut" est conservé à l'identique.
    private List<AlerteAnticipation> calculerParType(TypeAnticipation type, Integer prevenanceJours,
                                                      Integer retardJours, StatutAgent statut) {
        FenetreAnticipation resolue = configurationDelaiService.resoudreFenetre(type);
        FenetreAnticipation fenetre = new FenetreAnticipation(
                prevenanceJours != null ? prevenanceJours : resolue.prevenanceJours(),
                retardJours != null ? retardJours : resolue.retardJours());

        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == type)
                        .map(e -> versAlerte(a, e)))
                .filter(al -> fenetre.contient(al.joursRestants()))   // MODIFIÉ
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    // versAlerte() et formatRetard() — inchangées
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
