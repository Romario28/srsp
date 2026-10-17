package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.dto.anticipation.AgentFicheDTO;
import com.entreprise.gestion.exception.BusinessException; // AJOUTÉ
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnticipationService {

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final ConfigurationDelaiService configurationDelaiService;
    private final GradeSuivantService gradeSuivantService;

    /** Nombre total d'agents importés, quel que soit leur statut administratif. */
    @Transactional(readOnly = true)
    public long compterAgents() {
        return agentRepository.count();
    }

    @Transactional(readOnly = true)
    public AgentFicheDTO fiche(String matricule) {
        Agent agent = agentRepository.findById(matricule)
                .orElseThrow(() -> new BusinessException("AGENT_INTROUVABLE", "Agent introuvable : " + matricule));
        return AgentFicheDTO.from(agent, integrationDe(agent));
    }

    private AgentFicheDTO.Integration integrationDe(Agent agent) {
        RegleIntegration.Analyse analyse = RegleIntegration.analyser(agent);
        LocalDate preparation = analyse.dateIntegration() == null ? null
                : configurationDelaiService.resoudreFenetre(TypeAnticipation.INTEGRATION)
                        .datePreparation(analyse.dateIntegration());
        return new AgentFicheDTO.Integration(analyse.situation(), analyse.dateIntegration(), preparation);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> departsRetraite(Integer prevenanceMois, Integer retardMois,
                                                      LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                      FiltreAgents filtre) {
        return calculerParType(TypeAnticipation.DEPART_RETRAITE, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> avancementsDus(Integer prevenanceMois, Integer retardMois,
                                                     LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                     FiltreAgents filtre) {
        return calculerParType(TypeAnticipation.AVANCEMENT, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> titularisationsDues(Integer prevenanceMois, Integer retardMois,
                                                          LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                          FiltreAgents filtre) {
        return calculerParType(TypeAnticipation.TITULARISATION, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> finsContrat(Integer prevenanceMois, Integer retardMois,
                                                  LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                  FiltreAgents filtre) {
        return calculerParType(TypeAnticipation.FIN_CONTRAT, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> integrationsDues(Integer prevenanceMois, Integer retardMois,
            LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate, FiltreAgents filtre) {
        return calculerParType(TypeAnticipation.INTEGRATION, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> anomalies(FiltreAgents filtre) {
        return lignes(anomaliesAvecAgent(filtre));
    }

    public record AgentEcheance(Agent agent, Echeance echeance, EcheanceAnticipeeDTO ligne) {}

    @Transactional(readOnly = true)
    public List<AgentEcheance> anomaliesAvecAgent(FiltreAgents filtre) {
        return agentRepository.findAllActifs().stream()
                .filter(filtre::accepte)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> new AgentEcheance(a, e, versAlerte(a, e, null, null))))
                .collect(Collectors.toList());
    }

    private static List<EcheanceAnticipeeDTO> lignes(List<AgentEcheance> echeances) {
        return echeances.stream().map(AgentEcheance::ligne).collect(Collectors.toList());
    }

    /**
     * Sans intervalle absolu, seuls les agents dont la fenêtre [préparation ; échéance + tolérance]
     * contient aujourd'hui sont retenus. Avec dateDebut/dateFin, le critère choisi filtre l'échéance
     * ou sa date de préparation ; le délai de préparation reste résolu pour calculer celle-ci.
     */
    public FenetreAnticipation fenetreEffective(TypeAnticipation type, Integer prevenanceMois, Integer retardMois) {
        FenetreAnticipation resolue = configurationDelaiService.resoudreFenetre(type);
        return new FenetreAnticipation(prevenanceMois != null ? prevenanceMois : resolue.prevenanceMois(),
                retardMois != null ? retardMois : resolue.retardMois());
    }

    @Transactional(readOnly = true)
    public List<AgentEcheance> echeancesAvecAgent(TypeAnticipation type, Integer prevenanceMois, Integer retardMois,
            LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate, FiltreAgents filtre) {

        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin précède la date de début.");
        }
        boolean filtreDatesActif = dateDebut != null || dateFin != null;
        boolean surPreparation = critereDate == CritereDate.PREPARATION;
        LocalDate aujourdhui = LocalDate.now();
        FenetreAnticipation fenetre = fenetreEffective(type, prevenanceMois, retardMois);
        FenetreAnticipation fenetrePreparation = surPreparation
                ? configurationDelaiService.resoudreFenetre(type) : fenetre;
        GradesParCorps grades = type == TypeAnticipation.AVANCEMENT || type == TypeAnticipation.TITULARISATION
                ? gradeSuivantService.indexer() : null;
        return agentRepository.findAllActifs().stream().filter(filtre::accepte)
                .flatMap(a -> moteur.calculerEcheances(a).stream().filter(e -> e.type() == type)
                        .map(e -> new AgentEcheance(a, e, versAlerte(a, e, fenetrePreparation, grades))))
                .filter(ae -> {
                    LocalDate retenue = surPreparation ? ae.ligne().datePreparation() : ae.ligne().dateEcheance();
                    return filtreDatesActif
                            ? dansIntervalle(retenue, dateDebut, dateFin)
                            : fenetre.contient(retenue, aujourdhui);
                })
                .sorted(Comparator.comparingLong(ae -> ae.ligne().joursRestants())).collect(Collectors.toList());
    }

    private List<EcheanceAnticipeeDTO> calculerParType(TypeAnticipation type, Integer prevenanceMois,
                                                       Integer retardMois, LocalDate dateDebut, LocalDate dateFin,
                                                       CritereDate critereDate, FiltreAgents filtre) {
        return lignes(echeancesAvecAgent(type, prevenanceMois, retardMois,
                dateDebut, dateFin, critereDate, filtre));
    }

    // AJOUTÉ — une échéance de type non-ANOMALIE a toujours une dateEcheance non nulle
    // (voir MoteurAnticipation) ; le null-check n'est qu'une garde défensive.
    private boolean dansIntervalle(LocalDate date, LocalDate dateDebut, LocalDate dateFin) {
        if (date == null) return false;
        if (dateDebut != null && date.isBefore(dateDebut)) return false;
        if (dateFin != null && date.isAfter(dateFin)) return false;
        return true;
    }

    private EcheanceAnticipeeDTO versAlerte(Agent a, Echeance e, FenetreAnticipation fenetre,
                                             GradesParCorps grades) {
        return EcheanceAnticipeeDTO.de(a, e, fenetre, grades);
    }

}
