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
 * Vue « anticipation » calculée à la volée pour les endpoints API.
 * La fenêtre de visibilité de chaque type n'est jamais codée ici : elle vient de
 * {@link ConfigurationDelaiService#resoudreFenetre} (base si active, sinon défaut),
 * exactement comme pour le batch nocturne.
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

    /** Les anomalies n'ont pas de fenêtre : elles sont toujours remontées. */
    @Transactional(readOnly = true)
    public List<AlerteAnticipation> anomalies(StatutAgent statut) {
        LocalDate aujourdhui = LocalDate.now();
        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> versAlerte(a, e, aujourdhui)))
                .collect(Collectors.toList());
    }

    /**
     * Un agent n'est visible que si {@code joursRestants ∈ [-retard, +prévenance]}.
     * Les bornes viennent de la configuration (base active, sinon défaut) ; un paramètre
     * de requête non nul ne surcharge que la borne correspondante, pour un appel ponctuel.
     */
    private List<AlerteAnticipation> calculerParType(TypeAnticipation type,
                                                     Integer prevenanceDemandee,
                                                     Integer retardDemande,
                                                     StatutAgent statut) {
        FenetreAnticipation fenetre = resoudreFenetre(type, prevenanceDemandee, retardDemande);
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

    /** Fenêtre effective du type, éventuellement ajustée par les bornes demandées dans la requête. */
    private FenetreAnticipation resoudreFenetre(TypeAnticipation type, Integer prevenanceDemandee, Integer retardDemande) {
        FenetreAnticipation fenetre = configurationDelaiService.resoudreFenetre(type);

        if (prevenanceDemandee != null) {
            if (prevenanceDemandee < 0) {
                throw new BusinessException("PREVENANCE_INVALIDE", "La prévenance doit être positive ou nulle.");
            }
            fenetre = fenetre.avecPrevenance(prevenanceDemandee);
        }
        if (retardDemande != null) {
            if (retardDemande < 0) {
                throw new BusinessException("RETARD_INVALIDE", "Le retard doit être positif ou nul.");
            }
            fenetre = fenetre.avecRetard(retardDemande);
        }
        return fenetre;
    }

    private AlerteAnticipation versAlerte(Agent a, Echeance e, LocalDate aujourdhui) {
        long jours = e.dateEcheance() != null
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
