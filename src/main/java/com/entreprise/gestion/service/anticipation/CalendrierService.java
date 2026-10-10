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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CalendrierService {

    private static final int ANNEE_MIN = 1900;
    private static final int ANNEE_MAX = 2200;

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final ConfigurationDelaiService configurationDelaiService;

    private record Placee(Agent agent, CategorieCalendrier categorie,
                          LocalDate datePreparation, LocalDate dateEcheance) {}
    private record Cle(LocalDate datePreparation, LocalDate dateEcheance, CategorieCalendrier categorie) {}

    /** Toutes les échéances liées à l'année, avec leurs deux dates, en un parcours des agents actifs. */
    @Transactional(readOnly = true)
    public List<CalendrierJourDTO> annee(int annee) {
        if (annee < ANNEE_MIN || annee > ANNEE_MAX) {
            throw new BusinessException("ANNEE_INVALIDE",
                    "L'année doit être comprise entre " + ANNEE_MIN + " et " + ANNEE_MAX + ".");
        }
        return placees()
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

    /** Détail à la demande d'une journée selon la date choisie par le client. */
    @Transactional(readOnly = true)
    public List<CalendrierAgentDTO> agents(LocalDate jour, CritereDate critere) {
        boolean surPreparation = (critere != null ? critere : CritereDate.PREPARATION) == CritereDate.PREPARATION;
        return placees()
                .filter(p -> jour.equals(surPreparation ? p.datePreparation() : p.dateEcheance()))
                .map(p -> new CalendrierAgentDTO(
                        p.agent().getMatricule(), p.agent().getPrenoms() + " " + p.agent().getNom(),
                        p.categorie(), p.agent().getStatut(), p.datePreparation(), p.dateEcheance()))
                .sorted(Comparator.comparing(CalendrierAgentDTO::categorie)
                        .thenComparing(CalendrierAgentDTO::nomComplet))
                .toList();
    }

    /** Échéances datées des agents actifs, sans appliquer la fenêtre de visibilité des alertes. */
    private Stream<Placee> placees() {
        Map<TypeAnticipation, FenetreAnticipation> fenetres = configurationDelaiService.resoudreToutes();
        return agentRepository.findAllActifs().stream()
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() != TypeAnticipation.ANOMALIE && e.dateEcheance() != null)
                        .map(e -> new Placee(a, CategorieCalendrier.de(e.type(), a.getStatut()),
                                fenetres.get(e.type()).datePreparation(e.dateEcheance()), e.dateEcheance())));
    }
}
