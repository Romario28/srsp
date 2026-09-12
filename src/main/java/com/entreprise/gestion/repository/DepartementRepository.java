package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.entite.Employe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {

    Optional<Departement> findByChefId(Long chefId);   // unique=true → au plus un résultat
    List<Departement> findByDepartementParentId(Long parentId);
    List<Departement> findByCheminStartingWith(String chemin);

    /** Département dont cet employé est le chef — au plus 1, contrainte unique sur id_chef. */
    Optional<Departement> findByChef(Employe chef);

    /** Enfants directs — utilisé pour la recalculation récursive du chemin. */
    List<Departement> findByDepartementParent(Departement parent);

    /** Racine(s) de la hiérarchie — normalement un seul (Secrétariat Général). */
    List<Departement> findByDepartementParentIsNull();

    Optional<Departement> findByNomDepartement(String nomDepartement);

}