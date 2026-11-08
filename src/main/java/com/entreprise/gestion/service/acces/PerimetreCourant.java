package com.entreprise.gestion.service.acces;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Périmètre du compte qui fait la requête en cours, lu dans le contexte de sécurité.
 * Sans compte authentifié (traitement planifié, test) : aucun agent. Jamais d'accès par défaut.
 */
@Component
@RequiredArgsConstructor
public class PerimetreCourant {

    private final ControleAcces controleAcces;

    public Perimetre get() {
        Authentication authentification = SecurityContextHolder.getContext().getAuthentication();
        if (authentification != null && authentification.getPrincipal() instanceof UserDetailsImpl acteur) {
            return controleAcces.perimetreDe(acteur);
        }
        return Perimetre.aucun();
    }

    /** Refuse (403) si l'agent est hors périmètre : à appeler sur toute route qui reçoit un identifiant. */
    public void exiger(Agent agent) {
        if (!get().autorise(agent)) throw new AccessDeniedException("Cet agent est hors de votre périmètre.");
    }
}
