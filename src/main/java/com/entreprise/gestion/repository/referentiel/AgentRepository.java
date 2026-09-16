package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.service.anticipation.CodesSituationAdministrative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, String> {

    List<Agent> findByStatutAndDateNaissanceLessThanEqual(StatutAgent statut, LocalDate limite);

    @Query("SELECT a FROM Agent a JOIN FETCH a.corps JOIN FETCH a.grade " +
            "WHERE a.avanceDate IS NOT NULL")
    List<Agent> findAllWithCorpsGradeForAvancement();

    @Query("SELECT a FROM Agent a WHERE a.sanction IS NULL OR a.sanction.code = '"
            + CodesSituationAdministrative.CODE_ACTIF + "'")
    List<Agent> findAllActifs();
}
