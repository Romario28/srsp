package com.entreprise.gestion.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Flux d'événements (SSE) vers les validateurs connectés (ADMIN, CHEF).
 * Le flux ne transporte AUCUNE donnée : il dit seulement « les demandes ont changé ». Le navigateur redemande alors
 * le compteur, que le serveur calcule avec le périmètre du compte. Le registre est en mémoire : une seule instance du serveur.
 */
@Component
public class DemandesComptesDiffusion {

    /** Un flux est fermé au bout de ce délai ; le navigateur se reconnecte, ce qui revalide le jeton et la session. */
    private static final long DUREE_MAX_MS = 10 * 60_000L;
    /** Un compte qui ouvre davantage de flux (onglets, boucle de reconnexion) voit les plus anciens fermés. */
    private static final int MAX_PAR_COMPTE = 5;

    private record Abonne(Long compte, SseEmitter emetteur) {}
    private final List<Abonne> abonnes = new CopyOnWriteArrayList<>();

    public SseEmitter abonner(Long compte) {
        List<Abonne> memeCompte = abonnes.stream().filter(a -> a.compte().equals(compte)).toList();
        for (int i = 0; i < memeCompte.size() - MAX_PAR_COMPTE + 1; i++) {
            Abonne ancien = memeCompte.get(i);
            abonnes.remove(ancien);
            ancien.emetteur().complete();
        }
        SseEmitter emetteur = new SseEmitter(DUREE_MAX_MS);
        Abonne abonne = new Abonne(compte, emetteur);
        abonnes.add(abonne);
        Runnable retirer = () -> abonnes.remove(abonne);
        emetteur.onCompletion(retirer);
        emetteur.onTimeout(() -> { retirer.run(); emetteur.complete(); });
        emetteur.onError(erreur -> retirer.run());
        envoyer(abonne, SseEmitter.event().name("pret").data("ok"));   // confirme l'ouverture ; le navigateur relit alors le compteur
        return emetteur;
    }

    /** Appelé une fois la transaction VALIDÉE : le compteur relu par les navigateurs est alors à jour. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void surChangement(DemandesComptesModifiees evenement) {
        notifier();
    }

    void notifier() {
        for (Abonne a : abonnes) envoyer(a, SseEmitter.event().name("changement").data("1"));
    }

    /** Un commentaire SSE régulier : garde la connexion ouverte à travers les proxys et détecte les clients partis. */
    @Scheduled(fixedRate = 25_000)
    void battement() {
        for (Abonne a : abonnes) envoyer(a, SseEmitter.event().comment("battement"));
    }

    int nombreAbonnes() { return abonnes.size(); }

    private void envoyer(Abonne abonne, SseEmitter.SseEventBuilder evenement) {
        try {
            abonne.emetteur().send(evenement);
        } catch (Exception e) {   // client parti (IOException) ou flux déjà terminé
            abonnes.remove(abonne);
        }
    }
}
