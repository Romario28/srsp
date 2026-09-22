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
import java.time.temporal.ChronoUnit;
import java.util.List;

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
public class BatchAnticipation {


    private static final int TAILLE_LOT = 500;

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final AlerteRepository alerteRepository;
    private final ConfigurationDelaiService configurationDelaiService; // ← remplace ConfigurationDelaiRepository
    private final EntityManager entityManager;

    @Scheduled(cron = "${anticipation.batch.cron:0 0 3 * * *}")
    @Transactional
    public void executer() {
        LocalDate aujourdhui = LocalDate.now();

        int compteur = 0;
        for (Agent agent : agentRepository.findAllActifs()) {
            for (Echeance e : moteur.calculerEcheances(agent)) {
                traiter(e, aujourdhui);
            }
            if (++compteur % TAILLE_LOT == 0) {
                entityManager.flush();
                entityManager.clear();
            }
        }
    }

    // MODIFIÉ — remplace le seuil unique "resoudreDelai(type):int" + "joursRestants > delai"
    // par la fenêtre à deux bornes, via la même méthode contient() que le service API.
    private void traiter(Echeance e, LocalDate aujourdhui) {
        if (e.type() == TypeAnticipation.ANOMALIE) {
            upsert(e, null);
            return;
        }

        FenetreAnticipation fenetre = configurationDelaiService.resoudreFenetre(e.type());   // MODIFIÉ
        long joursRestants = ChronoUnit.DAYS.between(aujourdhui, e.dateEcheance());
        if (!fenetre.contient(joursRestants)) return;                                          // MODIFIÉ

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