package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.PerimetreUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PerimetreUtilisateurRepository extends JpaRepository<PerimetreUtilisateur, Long> {
    List<PerimetreUtilisateur> findByUtilisateurId(Long idUtilisateur);
}
