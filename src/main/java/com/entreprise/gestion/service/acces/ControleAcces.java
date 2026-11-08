package com.entreprise.gestion.service.acces;

import com.entreprise.gestion.config.anticipation.DossierProperties;
import com.entreprise.gestion.entite.PerimetreUtilisateur;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.PerimetreUtilisateurRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Seul point de décision sur les droits d'agent. Le lien compte → agent (matricule_agent) n'ouvre aucun droit
 * de périmètre : il sert à la règle du conflit d'intérêts et à l'espace personnel de l'agent.
 */
@Component
@RequiredArgsConstructor
public class ControleAcces {

    private final PerimetreUtilisateurRepository perimetreRepository;
    private final DossierProperties proprietes;

    /** ADMIN : accès total, codé en dur. AGENT : aucun (il n'a que son espace). RH et CHEF : les lignes de PerimetreUtilisateur. */
    public Perimetre perimetreDe(UserDetailsImpl acteur) {
        if (acteur.hasRole("ADMIN")) return Perimetre.tout();
        if (acteur.hasRole("AGENT")) return Perimetre.aucun();
        Set<String> ministeres = new HashSet<>(), soas = new HashSet<>(), fivs = new HashSet<>();
        for (PerimetreUtilisateur p : perimetreRepository.findByUtilisateurId(acteur.getId())) {
            switch (p.getDimension()) {
                case MINISTERE -> ministeres.add(p.getCode());
                case SOA -> soas.add(p.getCode());
                case FIV -> fivs.add(p.getCode());
            }
        }
        return new Perimetre(false, ministeres, soas, fivs);
    }

    /** Vrai si l'un des rôles du compte est listé dans dossiers.roles-exemptes-conflit (vide par défaut). */
    public boolean exempteDuConflit(UserDetailsImpl acteur) {
        Set<String> exemptes = proprietes.getRolesExemptesConflit();
        return exemptes != null && !exemptes.isEmpty() && acteur.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).anyMatch(exemptes::contains);
    }

    /**
     * Démarrer, classer sans suite, rouvrir.
     * Agent : uniquement son propre dossier, et seulement si un administrateur lui a accordé le droit.
     * Personnel (ADMIN, RH, CHEF) : l'agent doit être dans le périmètre, et ne pas être soi-même.
     */
    public void verifierActionSurDossier(UserDetailsImpl acteur, Agent agent, String action) {
        String mien = acteur.getMatriculeAgent();
        boolean estLeSien = mien != null && mien.equals(agent.getMatricule());

        if (acteur.hasRole("AGENT")) {
            if (!estLeSien) throw new AccessDeniedException("Un agent ne traite que son propre dossier.");
            if (!acteur.isAgirSurSonDossier()) {
                throw new BusinessException("LECTURE_SEULE",
                        "Votre compte est en lecture seule : vous ne pouvez pas " + action + " votre dossier.");
            }
            return;
        }
        if (!perimetreDe(acteur).autorise(agent)) {
            throw new AccessDeniedException("Cet agent est hors de votre périmètre.");
        }
        if (estLeSien && !exempteDuConflit(acteur)) {
            throw new BusinessException("DOSSIER_PROPRE",
                    "Conflit d'intérêts : vous ne pouvez pas " + action + " votre propre dossier.");
        }
    }
}
