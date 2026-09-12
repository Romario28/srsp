package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.Employe;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface EmployeRepository extends JpaRepository<Employe, Long>, JpaSpecificationExecutor<Employe> {

    Optional<Employe> findByMatricule(String matricule);

    List<Employe> findByDepartementId(Long idDepartement);

    boolean existsByMatricule(String matricule);

    // Vérifie si un compte est lié sans charger la relation lazy Utilisateur
    @Query("SELECT COUNT(u) > 0 FROM Utilisateur u WHERE u.employe.id = :employeId")
    boolean hasUtilisateur(Long employeId);

    @Query("SELECT e.id FROM Employe e WHERE e.utilisateur IS NOT NULL")
    Set<Long> findIdsWithUtilisateur();

    /** Charge `departement` en une requête — évite le N+1 sur departement.chemin. */
    @EntityGraph(attributePaths = {"departement"})
    @Override
    Page<Employe> findAll(Specification<Employe> spec, Pageable pageable);
}