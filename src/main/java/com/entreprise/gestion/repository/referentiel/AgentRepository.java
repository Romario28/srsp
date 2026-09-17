package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.service.anticipation.CodesSituationAdministrative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, String> {

    @Query("SELECT a FROM Agent a WHERE a.sanction IS NULL OR a.sanction.code = '"
            + CodesSituationAdministrative.CODE_ACTIF + "'")
    List<Agent> findAllActifs();
}
