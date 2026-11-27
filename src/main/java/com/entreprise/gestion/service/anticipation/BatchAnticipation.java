package com.entreprise.gestion.service.anticipation;

// MODIFIÉ : imports réduits (était : entite.anticipation.* + AlerteRepository + GradeSuivantService + List)
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.service.dossier.DossierGenerateur;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

/**
 * MODIFIÉ : processus planifié qui crée les dossiers dont la date de préparation est atteinte.
 * Le recalcul complet est idempotent : toute modification de données est prise en compte au passage suivant.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BatchAnticipation {

    // SUPPRIMÉ : private record SituationActuelle(String grade, LocalDate dateEffet) {}

    private static final int TAILLE_LOT = 500;

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    // SUPPRIMÉ : private final AlerteRepository alerteRepository;
    private final ConfigurationDelaiService configurationDelaiService;
    // SUPPRIMÉ : private final GradeSuivantService gradeSuivantService;
    private final EntityManager entityManager;
    private final DossierGenerateur dossierGenerateur;

    @Scheduled(cron = "${anticipation.batch.cron:0 */5 * * * *}")
    @Transactional
    public void executer() {
        LocalDate aujourdhui = LocalDate.now();
        Map<TypeAnticipation, FenetreAnticipation> fenetres = configurationDelaiService.resoudreToutes();
        // SUPPRIMÉ : GradesParCorps grades = gradeSuivantService.indexer();
        long debut = System.currentTimeMillis();
        // SUPPRIMÉ : long alertesAvant = alerteRepository.count();
        DossierGenerateur.Passage dossiers = dossierGenerateur.ouvrir(aujourdhui);

        int compteur = 0;
        for (Agent agent : agentRepository.findAllActifs()) {
            for (Echeance e : moteur.calculerEcheances(agent)) {
                // SUPPRIMÉ : traiter(agent, e, aujourdhui, fenetres, grades);
                dossiers.traiter(agent, e, fenetres.get(e.type()));
            }
            if (++compteur % TAILLE_LOT == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }

        int dossiersCrees = dossiers.terminer();
        // MODIFIÉ : était : "Batch anticipation : {} agent(s) traité(s), {} alerte(s) créée(s), {} dossier(s) créé(s), {} ms"
        log.info("Batch anticipation : {} agent(s) traité(s), {} dossier(s) créé(s), {} ms",
                compteur, dossiersCrees, System.currentTimeMillis() - debut);
    }

    // ─── SUPPRIMÉ : tout ce qui suit servait aux alertes ──────────────────────

    // private void traiter(Agent agent, Echeance e, LocalDate aujourdhui,
    //                      Map<TypeAnticipation, FenetreAnticipation> fenetres, GradesParCorps grades) {
    //     if (e.type() == TypeAnticipation.ANOMALIE) {
    //         upsertAnomalie(e);
    //         return;
    //     }
    //     FenetreAnticipation fenetre = fenetres.get(e.type());
    //     if (!fenetre.contient(e.dateEcheance(), aujourdhui)) return;
    //     boolean changeDeGrade = e.type().changeDeGrade();
    //     GradeSuivant gradeSuivant = changeDeGrade ? calculerGradeSuivant(agent, grades) : null;
    //     SituationActuelle situation = changeDeGrade ? situationActuelle(agent) : null;
    //     upsert(e, e.dateEcheance(), fenetre.datePreparation(e.dateEcheance()), gradeSuivant, situation);
    // }
    //
    // private SituationActuelle situationActuelle(Agent agent) {
    //     LocalDate dateEffet = agent.getAvanceDate() != null ? agent.getAvanceDate() : agent.getDateDebutContrat();
    //     String grade = agent.getGrade() != null ? agent.getGrade().getCode() : null;
    //     return new SituationActuelle(grade, dateEffet);
    // }
    //
    // private GradeSuivant calculerGradeSuivant(Agent agent, GradesParCorps grades) {
    //     if (agent.getGrade() == null || agent.getCorps() == null) return GradeSuivant.INDETERMINE;
    //     return grades.pour(agent.getCorps().getCode(), agent.getCorps().getCategorie(), agent.getGrade().getCode());
    // }
    //
    // private void upsertAnomalie(Echeance e) {
    //     boolean dejaConnue = alerteRepository
    //             .findByMatriculeAgentAndType(e.matricule(), TypeAnticipation.ANOMALIE).stream()
    //             .anyMatch(a -> java.util.Objects.equals(a.getDetails(), e.details()));
    //     if (dejaConnue) return;
    //     enregistrer(e, null, null, null, null);
    // }
    //
    // private void upsert(Echeance e, LocalDate dateEcheance, LocalDate datePreparation,
    //                     GradeSuivant gradeSuivant, SituationActuelle situation) {
    //     reclasserAnciennesAlertes(e, dateEcheance);
    //     List<Alerte> existantes = alerteRepository
    //             .findByMatriculeAgentAndTypeAndStatutNot(e.matricule(), e.type(), StatutAlerte.ACQUITTEE);
    //     Alerte presente = existantes.stream()
    //             .filter(a -> java.util.Objects.equals(a.getDateEcheance(), dateEcheance))
    //             .findFirst().orElse(null);
    //     if (presente != null) {
    //         boolean modifiee = false;
    //         if (presente.getDatePreparation() == null && datePreparation != null) { presente.setDatePreparation(datePreparation); modifiee = true; }
    //         if ((presente.getGradeSuivantCas() == null || presente.getGradeSuivant() == null) && gradeSuivant != null) {
    //             presente.setGradeSuivantCas(gradeSuivant.cas());
    //             presente.setGradeSuivant(gradeSuivant.valeurStockee());
    //             modifiee = true;
    //         }
    //         if (presente.getGradeActuel() == null && situation != null && situation.grade() != null) {
    //             presente.setGradeActuel(situation.grade());
    //             presente.setDateEffetActuelle(situation.dateEffet());
    //             modifiee = true;
    //         }
    //         if (modifiee) alerteRepository.save(presente);
    //         return;
    //     }
    //     boolean dejaAcquitteeIdentique = alerteRepository
    //             .existsByMatriculeAgentAndTypeAndStatutAndDateEcheance(e.matricule(), e.type(), StatutAlerte.ACQUITTEE, dateEcheance);
    //     if (dejaAcquitteeIdentique) return;
    //     enregistrer(e, dateEcheance, datePreparation, gradeSuivant, situation);
    // }
    //
    // private void reclasserAnciennesAlertes(Echeance e, LocalDate dateEcheance) {
    //     if (e.type() != TypeAnticipation.AVANCEMENT_CLASSE) return;
    //     for (Alerte ancienne : alerteRepository.findByMatriculeAgentAndType(e.matricule(), TypeAnticipation.AVANCEMENT)) {
    //         if (java.util.Objects.equals(ancienne.getDateEcheance(), dateEcheance)) {
    //             ancienne.setType(TypeAnticipation.AVANCEMENT_CLASSE);
    //             alerteRepository.save(ancienne);
    //         }
    //     }
    // }
    //
    // private void enregistrer(Echeance e, LocalDate dateEcheance, LocalDate datePreparation,
    //                          GradeSuivant gradeSuivant, SituationActuelle situation) {
    //     alerteRepository.save(Alerte.builder()
    //             .matriculeAgent(e.matricule()).nomCompletAgent(e.nomComplet()).type(e.type())
    //             .dateEcheance(dateEcheance).datePreparation(datePreparation)
    //             .gradeSuivantCas(gradeSuivant != null ? gradeSuivant.cas() : null)
    //             .gradeSuivant(gradeSuivant != null ? gradeSuivant.valeurStockee() : null)
    //             .gradeActuel(situation != null ? situation.grade() : null)
    //             .dateEffetActuelle(situation != null ? situation.dateEffet() : null)
    //             .details(e.details()).statut(StatutAlerte.NOUVELLE).build());
    // }
}
