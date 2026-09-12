package com.entreprise.gestion.service;

import com.entreprise.gestion.entite.Employe;
import com.entreprise.gestion.entite.PorteeDeleguee;
import com.entreprise.gestion.entite.TypeAcces;
import com.entreprise.gestion.repository.DepartementRepository;
import com.entreprise.gestion.repository.PorteeDelegueeRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.security.visibility.VisibilityScope;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

/**
 * Aucun rôle Spring Security dédié à la visibilité fine (ni MANAGER, ni
 * RH_LOCAL, ni RH_CENTRAL, ni STAKEHOLDER) : l'autorité est entièrement
 * dérivée de deux faits vérifiables en base —
 *   1. suis-je chef d'un département (Departement.chef) ?
 *   2. ai-je une PorteeDeleguee active sur un département ?
 * ROLE_ADMIN reste le seul rôle à donner un accès total codé en dur.
 */
@Service
@RequiredArgsConstructor
public class VisibiliteService {

    private final DepartementRepository    departementRepository;
    private final PorteeDelegueeRepository porteeDelegueeRepository;

    @Cacheable(value = "visibilityScope", key = "#principal.id")
    @Transactional(readOnly = true)
    public VisibilityScope resoudreScope(UserDetailsImpl principal) {

        if (principal.hasRole("ADMIN")) return VisibilityScope.tout();

        Set<String> cheminsLecture  = new HashSet<>();
        Set<String> cheminsEcriture = new HashSet<>();
        Set<Long>   employeIds      = new HashSet<>();

        Employe monEmploye = principal.getEmploye();

        if (monEmploye != null) {
            employeIds.add(monEmploye.getId());

            departementRepository.findByChefId(monEmploye.getId()).ifPresent(dept -> {
                cheminsLecture.add(dept.getChemin());
                cheminsEcriture.add(dept.getChemin());
            });
        }

        for (PorteeDeleguee portee : porteeDelegueeRepository
                .findByUtilisateurIdOrderByDateDebutDesc(principal.getId())) {

            if (!portee.estActive(LocalDate.now())) continue;

            String chemin = portee.getDepartement().getChemin();
            cheminsLecture.add(chemin);
            if (portee.getTypeAcces() == TypeAcces.LECTURE_ECRITURE) {
                cheminsEcriture.add(chemin);
            }
        }

        return VisibilityScope.restreint(cheminsLecture, cheminsEcriture, employeIds);
    }
}