package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.repository.anticipation.AlerteRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Processus quotidien qui satisfait l'exigence "recalcul automatique" :
 * plutôt qu'un déclenchement événementiel sur chaque écriture (Agent,
 * IndiceGrdCorps...), un recalcul complet et idempotent chaque nuit garantit
 * que toute modification de données est prise en compte au plus tard le
 * lendemain — suffisant ici, plus simple et plus robuste qu'une chaîne
 * d'événements à maintenir.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchAnticipation {

    private record SituationActuelle(String grade, LocalDate dateEffet) {}

    private static final int TAILLE_LOT = 500;

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final AlerteRepository alerteRepository;
    private final ConfigurationDelaiService configurationDelaiService; // ← remplace ConfigurationDelaiRepository
    private final GradeSuivantService gradeSuivantService;
    private final EntityManager entityManager;

    @Scheduled(cron = "${anticipation.batch.cron:0 */10 * * * *}")
    @Transactional
    public void executer() {
        LocalDate aujourdhui = LocalDate.now();
        Map<TypeAnticipation, FenetreAnticipation> fenetres = configurationDelaiService.resoudreToutes();
        GradesParCorps grades = gradeSuivantService.indexer();
        long debut = System.currentTimeMillis();
        long alertesAvant = alerteRepository.count();

        int compteur = 0;
        for (Agent agent : agentRepository.findAllActifs()) {
            for (Echeance e : moteur.calculerEcheances(agent)) {
                traiter(agent, e, aujourdhui, fenetres, grades);
            }
            if (++compteur % TAILLE_LOT == 0) {
                entityManager.flush();
                entityManager.clear();

            }
        }

        log.info("Batch anticipation : {} agent(s) traité(s), {} alerte(s) créée(s), {} ms",
                compteur, alerteRepository.count() - alertesAvant, System.currentTimeMillis() - debut);
    }

    // MODIFIÉ — remplace le seuil unique "resoudreDelai(type):int" + "joursRestants > delai"
    // par la fenêtre à deux bornes, via la même méthode contient() que le service API.
    private void traiter(Agent agent, Echeance e, LocalDate aujourdhui,
                         Map<TypeAnticipation, FenetreAnticipation> fenetres, GradesParCorps grades) {
        if (e.type() == TypeAnticipation.ANOMALIE) {
            upsertAnomalie(e);
            return;
        }

        FenetreAnticipation fenetre = fenetres.get(e.type());
        if (!fenetre.contient(e.dateEcheance(), aujourdhui)) return;

        boolean changeDeGrade = e.type() == TypeAnticipation.AVANCEMENT
                || e.type() == TypeAnticipation.TITULARISATION;
        GradeSuivant gradeSuivant = changeDeGrade ? calculerGradeSuivant(agent, grades) : null;
        SituationActuelle situation = changeDeGrade ? situationActuelle(agent) : null;
        upsert(e, e.dateEcheance(), fenetre.datePreparation(e.dateEcheance()), gradeSuivant, situation);
    }

    /** Dernier grade et date d'ancrage retenus au moment de la détection. */
    private SituationActuelle situationActuelle(Agent agent) {
        LocalDate dateEffet = agent.getAvanceDate() != null
                ? agent.getAvanceDate() : agent.getDateDebutContrat();
        String grade = agent.getGrade() != null ? agent.getGrade().getCode() : null;
        return new SituationActuelle(grade, dateEffet);
    }

    private GradeSuivant calculerGradeSuivant(Agent agent, GradesParCorps grades) {
        if (agent.getGrade() == null || agent.getCorps() == null) return GradeSuivant.INDETERMINE;
        return grades.pour(agent.getCorps().getCode(), agent.getCorps().getCategorie(), agent.getGrade().getCode());
    }

    private void upsertAnomalie(Echeance e) {
        boolean dejaConnue = alerteRepository
                .findByMatriculeAgentAndType(e.matricule(), TypeAnticipation.ANOMALIE).stream()
                .anyMatch(a -> java.util.Objects.equals(a.getDetails(), e.details()));
        if (dejaConnue) return;
        enregistrer(e, null, null, null, null);
    }
    /**
     * Une alerte déjà acquittée pour cette échéance exacte n'est jamais
     * recréée. Si la date d'échéance calculée diffère de celle acquittée
     * (donnée source corrigée entre-temps), une NOUVELLE alerte apparaît —
     * l'ancienne reste en base, satisfaisant la traçabilité historique.
     */
    private void upsert(Echeance e, LocalDate dateEcheance, LocalDate datePreparation,
                        GradeSuivant gradeSuivant, SituationActuelle situation) {
        List<Alerte> existantes = alerteRepository
                .findByMatriculeAgentAndTypeAndStatutNot(e.matricule(), e.type(), StatutAlerte.ACQUITTEE);

        Alerte presente = existantes.stream()
                .filter(a -> java.util.Objects.equals(a.getDateEcheance(), dateEcheance))
                .findFirst()
                .orElse(null);

        if (presente != null) {
            boolean modifiee = false;
            if (presente.getDatePreparation() == null && datePreparation != null) {
                presente.setDatePreparation(datePreparation);
                modifiee = true;
            }
            if ((presente.getGradeSuivantCas() == null || presente.getGradeSuivant() == null)
                    && gradeSuivant != null) {
                presente.setGradeSuivantCas(gradeSuivant.cas());
                presente.setGradeSuivant(gradeSuivant.valeurStockee());
                modifiee = true;
            }
            if (presente.getGradeActuel() == null && situation != null && situation.grade() != null) {
                presente.setGradeActuel(situation.grade());
                presente.setDateEffetActuelle(situation.dateEffet());
                modifiee = true;
            }
            if (modifiee) {
                alerteRepository.save(presente);
            }
            return;
        }

        boolean dejaAcquitteeIdentique = alerteRepository
                .existsByMatriculeAgentAndTypeAndStatutAndDateEcheance(
                        e.matricule(), e.type(), StatutAlerte.ACQUITTEE, dateEcheance);
        if (dejaAcquitteeIdentique) return;

        enregistrer(e, dateEcheance, datePreparation, gradeSuivant, situation);
    }

    private void enregistrer(Echeance e, LocalDate dateEcheance, LocalDate datePreparation,
                             GradeSuivant gradeSuivant, SituationActuelle situation) {
        alerteRepository.save(Alerte.builder()
                .matriculeAgent(e.matricule())
                .nomCompletAgent(e.nomComplet())
                .type(e.type())
                .dateEcheance(dateEcheance)
                .datePreparation(datePreparation)
                .gradeSuivantCas(gradeSuivant != null ? gradeSuivant.cas() : null)
                .gradeSuivant(gradeSuivant != null ? gradeSuivant.valeurStockee() : null)
                .gradeActuel(situation != null ? situation.grade() : null)
                .dateEffetActuelle(situation != null ? situation.dateEffet() : null)
                .details(e.details())
                .statut(StatutAlerte.NOUVELLE)
                .build());
    }
}
