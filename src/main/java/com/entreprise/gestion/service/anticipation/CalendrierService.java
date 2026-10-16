package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.dto.anticipation.CalendrierAgentDTO;
import com.entreprise.gestion.dto.anticipation.CalendrierJourDTO;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CalendrierService {

    private static final int ANNEE_MIN = 1900;
    private static final int ANNEE_MAX = 2200;
    private static final long MAX_JOURS_EXPORT = 366;

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final ConfigurationDelaiService configurationDelaiService;
    private final GradeSuivantService gradeSuivantService;

    private record Placee(Agent agent, Echeance echeance, CategorieCalendrier categorie, FenetreAnticipation fenetre) {
        LocalDate datePreparation() { return fenetre.datePreparation(echeance.dateEcheance()); }
        LocalDate dateEcheance() { return echeance.dateEcheance(); }
    }
    private record Cle(LocalDate datePreparation, LocalDate dateEcheance, CategorieCalendrier categorie) {}
    public record EcheancePlacee(Agent agent, CategorieCalendrier categorie, LocalDate datePreparation, LocalDate dateEcheance) {}

    /**
     * Une année du calendrier, en un seul parcours des agents : toutes les échéances dont la date de
     * préparation OU la date d'échéance tombe dans l'année, avec les deux dates. Le choix de la date
     * affichée se fait côté client, sans nouvel appel.
     */
    @Transactional(readOnly = true)
    public List<CalendrierJourDTO> annee(int annee, FiltreAgents filtre) {
        verifierAnnee(annee);
        return placees(filtre)
                .filter(p -> p.datePreparation().getYear() == annee || p.dateEcheance().getYear() == annee)
                .collect(Collectors.groupingBy(
                        p -> new Cle(p.datePreparation(), p.dateEcheance(), p.categorie()), Collectors.counting()))
                .entrySet().stream()
                .map(e -> new CalendrierJourDTO(e.getKey().datePreparation(), e.getKey().dateEcheance(),
                        e.getKey().categorie(), e.getValue()))
                .sorted(Comparator.comparing(CalendrierJourDTO::dateEcheance)
                        .thenComparing(CalendrierJourDTO::datePreparation)
                        .thenComparing(CalendrierJourDTO::categorie))
                .toList();
    }

    /** Détail d'un jour, appelé au clic : la ligne complète de chaque agent, comme sur les écrans d'échéances. */
    @Transactional(readOnly = true)
    public List<CalendrierAgentDTO> agents(LocalDate jour, CritereDate critere, FiltreAgents filtre) {
        return agentsEntre(jour, jour, critere, filtre);
    }

    /** Tous les agents d'un mois entier (mois : 1 à 12), même forme que le détail d'un jour. */
    @Transactional(readOnly = true)
    public List<CalendrierAgentDTO> agentsDuMois(int annee, int mois, CritereDate critere, FiltreAgents filtre) {
        verifierAnnee(annee);
        if (mois < 1 || mois > 12) {
            throw new BusinessException("MOIS_INVALIDE", "Le mois doit être compris entre 1 et 12.");
        }
        YearMonth periode = YearMonth.of(annee, mois);
        return agentsEntre(periode.atDay(1), periode.atEndOfMonth(), critere, filtre);
    }

    @Transactional(readOnly = true)
    public List<EcheancePlacee> pourExport(LocalDate debut, LocalDate fin, CritereDate critere,
            Set<CategorieCalendrier> types, FiltreAgents filtre) {
        if (debut == null || fin == null || fin.isBefore(debut))
            throw new BusinessException("DATES_INVALIDES", "La période du calendrier est invalide.");
        if (ChronoUnit.DAYS.between(debut, fin) > MAX_JOURS_EXPORT)
            throw new BusinessException("PERIODE_TROP_LONGUE", "La période ne peut pas dépasser une année.");
        boolean preparation = (critere != null ? critere : CritereDate.PREPARATION) == CritereDate.PREPARATION;
        return placees(filtre).filter(p -> {
                    LocalDate d = preparation ? p.datePreparation() : p.dateEcheance();
                    return !d.isBefore(debut) && !d.isAfter(fin);
                }).filter(p -> types == null || types.isEmpty() || types.contains(p.categorie()))
                .sorted(Comparator.comparing(Placee::categorie)
                        .thenComparing(preparation ? Placee::datePreparation : Placee::dateEcheance)
                        .thenComparing(p -> p.echeance().nomComplet()))
                .map(p -> new EcheancePlacee(p.agent(), p.categorie(), p.datePreparation(), p.dateEcheance())).toList();
    }

    /** Agents dont la date retenue (préparation ou échéance) tombe dans [debut ; fin], bornes incluses. */
    private List<CalendrierAgentDTO> agentsEntre(LocalDate debut, LocalDate fin, CritereDate critere, FiltreAgents filtre) {
        // Défaut : préparation, contrairement aux autres écrans d'anticipation (échéance).
        boolean surPreparation = (critere != null ? critere : CritereDate.PREPARATION) == CritereDate.PREPARATION;
        Function<Placee, LocalDate> dateRetenue = surPreparation ? Placee::datePreparation : Placee::dateEcheance;
        Function<EcheanceAnticipeeDTO, LocalDate> dateDeTri =
                surPreparation ? EcheanceAnticipeeDTO::datePreparation : EcheanceAnticipeeDTO::dateEcheance;
        GradesParCorps grades = gradeSuivantService.indexer();   // une requête par appel

        return placees(filtre)
                .filter(p -> {
                    LocalDate d = dateRetenue.apply(p);
                    return !d.isBefore(debut) && !d.isAfter(fin);
                })
                .map(p -> new CalendrierAgentDTO(p.categorie(), EcheanceAnticipeeDTO.de(
                        p.agent(), p.echeance(), p.fenetre(),
                        changeDeGrade(p.echeance().type()) ? grades : null)))
                .sorted(Comparator.comparing(CalendrierAgentDTO::categorie)
                        .thenComparing(d -> dateDeTri.apply(d.echeance()))
                        .thenComparing(d -> d.echeance().nomComplet()))
                .toList();
    }

    private static void verifierAnnee(int annee) {
        if (annee < ANNEE_MIN || annee > ANNEE_MAX) {
            throw new BusinessException("ANNEE_INVALIDE",
                    "L'année doit être comprise entre " + ANNEE_MIN + " et " + ANNEE_MAX + ".");
        }
    }

    private static boolean changeDeGrade(TypeAnticipation type) {
        return type == TypeAnticipation.AVANCEMENT || type == TypeAnticipation.TITULARISATION;
    }

    /** Échéances datées des agents en activité qui passent le filtre (les anomalies n'ont pas de date). */
    private Stream<Placee> placees(FiltreAgents filtre) {
        Map<TypeAnticipation, FenetreAnticipation> fenetres = configurationDelaiService.resoudreToutes();
        return agentRepository.findAllActifs().stream()
                .filter(filtre::accepte)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() != TypeAnticipation.ANOMALIE && e.dateEcheance() != null)
                        .map(e -> new Placee(a, e, CategorieCalendrier.de(e.type(), a.getStatut()),
                                fenetres.get(e.type()))));
    }
}
