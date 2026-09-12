package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.LogAudit;
import com.entreprise.gestion.entite.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LogAuditRepository extends JpaRepository<LogAudit, Long> {

    List<LogAudit> findByUtilisateurOrderByDateActionDesc(Utilisateur utilisateur);

    List<LogAudit> findAllByOrderByDateActionDesc();

    List<LogAudit> findByActionOrderByDateActionDesc(String action);
}
