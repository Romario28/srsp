package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException; // AJOUTÉ
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

    // MODIFIÉ — ajout dateDebut/dateFin (filtre absolu, prioritaire sur prevenanceJours/retardJours)
    @Transactional(readOnly = true)
    public List<AlerteAnticipation> departsRetraite(Integer prevenanceJours, Integer retardJours,
                                                     LocalDate dateDebut, LocalDate dateFin, StatutAgent statut) {
        return calculerParType(TypeAnticipation.DEPART_RETRAITE, prevenanceJours, retardJours, dateDebut, dateFin, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> avancementsDus(Integer prevenanceJours, Integer retardJours,
                                                    LocalDate dateDebut, LocalDate dateFin, StatutAgent statut) {
        return calculerParType(TypeAnticipation.AVANCEMENT, prevenanceJours, retardJours, dateDebut, dateFin, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> titularisationsDues(Integer prevenanceJours, Integer retardJours,
                                                         LocalDate dateDebut, LocalDate dateFin, StatutAgent statut) {
        return calculerParType(TypeAnticipation.TITULARISATION, prevenanceJours, retardJours, dateDebut, dateFin, statut);
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> finsContrat(Integer prevenanceJours, Integer retardJours,
                                                 LocalDate dateDebut, LocalDate dateFin, StatutAgent statut) {
        return calculerParType(TypeAnticipation.FIN_CONTRAT, prevenanceJours, retardJours, dateDebut, dateFin, statut);
    }

    // anomalies() — INCHANGÉE : pas de dateEcheance à filtrer (voir note en fin de réponse)
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
     * (DB active sinon défaut) ; non-null → surcharge ponctuelle de cet appel.
     *
     * dateDebut/dateFin (l'un des deux suffit) : filtre par intervalle de dates
     * ABSOLUES sur dateEcheance. PRIORITAIRE — s'il est actif, prevenanceJours/
     * retardJours et la configuration en base sont entièrement ignorés (même la
     * résolution de la fenêtre est court-circuitée, pas seulement son usage).
     */
    // MODIFIÉ — signature + corps : ajout dateDebut/dateFin, branchement filtre absolu vs fenêtre relative
    private List<AlerteAnticipation> calculerParType(TypeAnticipation type, Integer prevenanceJours, Integer retardJours,
                                                      LocalDate dateDebut, LocalDate dateFin, StatutAgent statut) {

        // AJOUTÉ — même validation que PorteeDelegueeService.accorder (code DATES_INVALIDES réutilisé)
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin précède la date de début.");
        }

        boolean filtreDatesActif = (dateDebut != null || dateFin != null);   // AJOUTÉ

        // AJOUTÉ — la fenêtre relative n'est résolue (donc aucune requête ConfigurationDelai)
        // que si le filtre par dates absolues n'est pas actif.
        FenetreAnticipation fenetre = null;
        if (!filtreDatesActif) {
            FenetreAnticipation resolue = configurationDelaiService.resoudreFenetre(type);
            fenetre = new FenetreAnticipation(
                    prevenanceJours != null ? prevenanceJours : resolue.prevenanceJours(),
                    retardJours != null ? retardJours : resolue.retardJours());
        }
        final FenetreAnticipation fenetreFinale = fenetre;

        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == type)
                        .map(e -> versAlerte(a, e)))
                // MODIFIÉ — avant : .filter(al -> fenetre.contient(al.joursRestants()))
                .filter(al -> filtreDatesActif
                        ? dansIntervalle(al.dateEcheance(), dateDebut, dateFin)
                        : fenetreFinale.contient(al.joursRestants()))
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    // AJOUTÉ — une échéance de type non-ANOMALIE a toujours une dateEcheance non nulle
    // (voir MoteurAnticipation) ; le null-check n'est qu'une garde défensive.
    private boolean dansIntervalle(LocalDate date, LocalDate dateDebut, LocalDate dateFin) {
        if (date == null) return false;
        if (dateDebut != null && date.isBefore(dateDebut)) return false;
        if (dateFin != null && date.isAfter(dateFin)) return false;
        return true;
    }

    // versAlerte() et formatRetard() — INCHANGÉES
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
