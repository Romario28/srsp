package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.PorteeDeleguee;
import com.entreprise.gestion.entite.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PorteeDelegueeRepository extends JpaRepository<PorteeDeleguee, Long> {


    List<PorteeDeleguee> findByUtilisateur(Utilisateur utilisateur);

    List<PorteeDeleguee> findByUtilisateurIdOrderByDateDebutDesc(Long utilisateurId);

    List<PorteeDeleguee> findByDepartementId(Long idDepartement);


}
