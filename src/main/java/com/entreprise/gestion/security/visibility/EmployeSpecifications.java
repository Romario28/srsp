package com.entreprise.gestion.security.visibility;

import com.entreprise.gestion.entite.Employe;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Construit la clause WHERE JPA Criteria à partir d'un VisibilityScope.
 * Le filtrage se fait ICI, au niveau de la requête SQL — jamais en
 * post-filtrant une liste Java déjà chargée en mémoire.
 */
public final class EmployeSpecifications {

    private EmployeSpecifications() {}

    public static Specification<Employe> visiblePar(VisibilityScope scope) {
        return (root, query, cb) -> {
            if (scope.isAccesTotal()) return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();

            if (!scope.getEmployeIdsAutorises().isEmpty()) {
                predicates.add(root.get("id").in(scope.getEmployeIdsAutorises()));
            }
            for (String chemin : scope.getCheminsLecture()) {
                predicates.add(cb.like(root.get("departement").get("chemin"), chemin + "%"));
            }
            if (predicates.isEmpty()) return cb.disjunction();

            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }
}