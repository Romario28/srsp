package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.Departement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DepartementRepository extends JpaRepository<Departement, Long> {
    List<Departement> findAllByOrderByLibelleAsc();
    boolean existsByLibelleIgnoreCase(String libelle);
    boolean existsByLibelleIgnoreCaseAndIdNot(String libelle, Long id);
}
