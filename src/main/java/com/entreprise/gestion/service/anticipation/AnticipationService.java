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

    /** Nombre total d'agents importés, quel que soit leur statut administratif. */
    @Transactional(readOnly = true)
    public long compterAgents() {
        return agentRepository.count();
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> departsRetraite(Integer prevenanceMois, Integer retardMois,
                                                      LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                      StatutAgent statut) {
        return calculerParType(TypeAnticipation.DEPART_RETRAITE, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, statut);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> avancementsDus(Integer prevenanceMois, Integer retardMois,
                                                     LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                     StatutAgent statut) {
        return calculerParType(TypeAnticipation.AVANCEMENT, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, statut);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> titularisationsDues(Integer prevenanceMois, Integer retardMois,
                                                          LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                          StatutAgent statut) {
        return calculerParType(TypeAnticipation.TITULARISATION, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, statut);
    }

    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> finsContrat(Integer prevenanceMois, Integer retardMois,
                                                  LocalDate dateDebut, LocalDate dateFin, CritereDate critereDate,
                                                  StatutAgent statut) {
        return calculerParType(TypeAnticipation.FIN_CONTRAT, prevenanceMois, retardMois, dateDebut, dateFin, critereDate, statut);
    }

    // anomalies() — INCHANGÉE : pas de dateEcheance à filtrer (voir note en fin de réponse)
    @Transactional(readOnly = true)
    public List<EcheanceAnticipeeDTO> anomalies(StatutAgent statut) {
        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == TypeAnticipation.ANOMALIE)
                        .map(e -> versAlerte(a, e, null)))
                .collect(Collectors.toList());
    }

    /**
     * Sans intervalle absolu, seuls les agents dont la fenêtre [préparation ; échéance + tolérance]
     * contient aujourd'hui sont retenus. Avec dateDebut/dateFin, le critère choisi filtre l'échéance
     * ou sa date de préparation ; le délai de préparation reste résolu pour calculer celle-ci.
     */
    private List<EcheanceAnticipeeDTO> calculerParType(TypeAnticipation type, Integer prevenanceMois,
                                                       Integer retardMois, LocalDate dateDebut, LocalDate dateFin,
                                                       CritereDate critereDate, StatutAgent statut) {

        // AJOUTÉ — même validation que PorteeDelegueeService.accorder (code DATES_INVALIDES réutilisé)
        if (dateDebut != null && dateFin != null && dateFin.isBefore(dateDebut)) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin précède la date de début.");
        }

        boolean filtreDatesActif = (dateDebut != null || dateFin != null);   // AJOUTÉ
        CritereDate critere = critereDate != null ? critereDate : CritereDate.ECHEANCE;
        LocalDate aujourdhui = LocalDate.now();

        FenetreAnticipation resolue = configurationDelaiService.resoudreFenetre(type);
        FenetreAnticipation fenetre = new FenetreAnticipation(
                prevenanceMois != null ? prevenanceMois : resolue.prevenanceMois(),
                retardMois != null ? retardMois : resolue.retardMois());

        return agentRepository.findAllActifs().stream()
                .filter(a -> statut == null || a.getStatut() == statut)
                .flatMap(a -> moteur.calculerEcheances(a).stream()
                        .filter(e -> e.type() == type)
                        .map(e -> versAlerte(a, e, fenetre)))
                .filter(al -> filtreDatesActif
                        ? dansIntervalle(critere == CritereDate.PREPARATION
                                ? al.datePreparation() : al.dateEcheance(), dateDebut, dateFin)
                        : fenetre.contient(al.dateEcheance(), aujourdhui))
                .sorted(Comparator.comparingLong(EcheanceAnticipeeDTO::joursRestants))
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

    private EcheanceAnticipeeDTO versAlerte(Agent a, Echeance e, FenetreAnticipation fenetre) {
        long jours = e.dateEcheance() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), e.dateEcheance())
                : 0;

        return new EcheanceAnticipeeDTO(
                a.getMatricule(),
                a.getPrenoms() + " " + a.getNom(),
                e.type(),
                e.dateEcheance(),
                fenetre != null ? fenetre.datePreparation(e.dateEcheance()) : null,
                jours,
                e.details(),
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

}
