package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.repository.anticipation.AlerteRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
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
 *
 * <p>La sélection des échéances utilise exactement la même fenêtre
 * prévenance/retard que les endpoints de consultation, résolue par
 * {@link ConfigurationDelaiService} : une alerte n'est créée que si l'échéance
 * tombe dans la fenêtre de son type. Les anomalies sont toujours remontées,
 * sans fenêtre.</p>
 *
 * <p>Une alerte déjà créée n'est pas supprimée lorsqu'une échéance sort ensuite
 * de la fenêtre : elle conserve son statut (NOUVELLE, VUE, ACQUITTEE) et reste
 * consultable via {@code /api/alertes} — l'historique et l'acquittement ne sont
 * jamais perdus.</p>
 */
@Component
@RequiredArgsConstructor
public class BatchAnticipation {


    private static final int TAILLE_LOT = 500;

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final AlerteRepository alerteRepository;
    private final ConfigurationDelaiService configurationDelaiService;
    private final EntityManager entityManager;

    @Scheduled(cron = "${anticipation.batch.cron:0 0 3 * * *}")
    @Transactional
    public void executer() {
        LocalDate aujourdhui = LocalDate.now();

        // Résolues une seule fois pour tout le batch : autant de requêtes que de types,
        // et une règle strictement identique à celle des endpoints API.
        Map<TypeAnticipation, FenetreAnticipation> fenetres = configurationDelaiService.resoudreToutes();

        int compteur = 0;
        for (Agent agent : agentRepository.findAllActifs()) {
            for (Echeance e : moteur.calculerEcheances(agent)) {
                traiter(e, aujourdhui, fenetres);
            }
            if (++compteur % TAILLE_LOT == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    private void traiter(Echeance e, LocalDate aujourdhui, Map<TypeAnticipation, FenetreAnticipation> fenetres) {
        if (e.type() == TypeAnticipation.ANOMALIE) {
            upsert(e, null);
            return;
        }

        FenetreAnticipation fenetre = fenetres.get(e.type());
        if (fenetre == null || !fenetre.contient(aujourdhui, e.dateEcheance())) return;

        upsert(e, e.dateEcheance());
    }
    /**
     * Une alerte déjà acquittée pour cette échéance exacte n'est jamais
     * recréée. Si la date d'échéance calculée diffère de celle acquittée
     * (donnée source corrigée entre-temps), une NOUVELLE alerte apparaît —
     * l'ancienne reste en base, satisfaisant la traçabilité historique.
     */
    private void upsert(Echeance e, LocalDate dateEcheance) {
        List<Alerte> existantes = alerteRepository
                .findByMatriculeAgentAndTypeAndStatutNot(e.matricule(), e.type(), StatutAlerte.ACQUITTEE);

        boolean dejaPresente = existantes.stream()
                .anyMatch(a -> java.util.Objects.equals(a.getDateEcheance(), dateEcheance));
        if (dejaPresente) return;

        boolean dejaAcquitteeIdentique = alerteRepository
                .existsByMatriculeAgentAndTypeAndStatutAndDateEcheance(
                        e.matricule(), e.type(), StatutAlerte.ACQUITTEE, dateEcheance);
        if (dejaAcquitteeIdentique) return;

        alerteRepository.save(Alerte.builder()
                .matriculeAgent(e.matricule())
                .nomCompletAgent(e.nomComplet())
                .type(e.type())
                .dateEcheance(dateEcheance)
                .details(e.details())
                .statut(StatutAlerte.NOUVELLE)
                .build());
    }
}
