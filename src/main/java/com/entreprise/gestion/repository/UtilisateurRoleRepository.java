package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.UtilisateurRole;
import com.entreprise.gestion.entite.UtilisateurRoleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UtilisateurRoleRepository extends JpaRepository<UtilisateurRole, UtilisateurRoleId> {
    List<UtilisateurRole> findByUtilisateurId(Long utilisateurId);
    void deleteByUtilisateurId(Long utilisateurId);
}
