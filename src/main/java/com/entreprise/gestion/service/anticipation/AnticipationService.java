package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Consultation des agents dont une échéance de carrière tombe dans la fenêtre
 * d'anticipation de son type.
 *
 * <p>La fenêtre appliquée est celle résolue par {@link ConfigurationDelaiService}
 * (configuration active en base, sinon défaut) : c'est exactement la règle utilisée
 * par le batch nocturne. Les paramètres de requête ne font que la surcharger
 * ponctuellement, ils ne la remplacent pas.</p>
 */
@Service
@RequiredArgsConstructor
public class AnticipationService {

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final ConfigurationDelaiService configurationDelaiService;

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

    /** Les anomalies de donnée n'ont pas d'échéance : elles sont toujours remontées. */
    @Transactional(readOnly = true)
    public List<AlerteAnticipation> anomalies(StatutAgent statut) {
        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> versAlerte(a, e, null)))
                .collect(Collectors.toList());
    }

    /**
     * Un agent est retenu si son écart à l'échéance tombe dans la fenêtre
     * {@code [-retard, +prevenance]} : {@code joursRestants} positif pour une échéance à
     * venir, négatif pour un retard, et l'agent disparaît de la liste au-delà des bornes.
     */
    private List<AlerteAnticipation> calculerParType(TypeAnticipation type,
                                                    Integer prevenanceJours,
                                                    Integer retardJours,
                                                    StatutAgent statut) {
        FenetreAnticipation fenetre = resoudreFenetre(type, prevenanceJours, retardJours);
        LocalDate aujourdhui = LocalDate.now();

        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == type)
                        .map(e -> versAlerte(a, e, aujourdhui)))
                .filter(al -> fenetre.contient(al.joursRestants()))
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    /** Fenêtre configurée (base ou défaut), éventuellement resserrée/élargie par l'appelant. */
    private FenetreAnticipation resoudreFenetre(TypeAnticipation type,
                                                Integer prevenanceJours,
                                                Integer retardJours) {
        if (prevenanceJours != null && prevenanceJours < 0) {
            throw new BusinessException("PREVENANCE_INVALIDE", "La prévenance doit être positive ou nulle.");
        }
        if (retardJours != null && retardJours < 0) {
            throw new BusinessException("RETARD_INVALIDE",
                    "Le retard doit être positif ou nul (0 = ne pas afficher les retards).");
        }
        return configurationDelaiService.resoudrePlage(type).surcharger(prevenanceJours, retardJours);
    }

    private AlerteAnticipation versAlerte(Agent a, Echeance e, LocalDate aujourdhui) {
        long jours = (e.dateEcheance() != null && aujourdhui != null)
                ? FenetreAnticipation.joursRestants(aujourdhui, e.dateEcheance())
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
