package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public interface AgentRepository extends JpaRepository<Agent, String> {

    List<Agent> findByStatutAndDateNaissanceLessThanEqual(StatutAgent statut, LocalDate limite);

    @Query("SELECT a FROM Agent a JOIN FETCH a.corps JOIN FETCH a.grade " +
            "WHERE a.avanceDate IS NOT NULL")
    List<Agent> findAllWithCorpsGradeForAvancement();


//    @Query("SELECT a FROM Agent a WHERE a.sanction IS NULL OR a.sanction.code NOT IN :codesSortie")
//    List<Agent> findAllActifs(@Param("codesSortie") java.util.Set<String> codesSortie);

    @Query("SELECT a FROM Agent a WHERE a.sanction IS NULL OR a.sanction.code NOT IN :codesSortie")
    List<Agent> findAllActifs(@Param("codesSortie") Set<String> codesSortie);   // ← celle-ci est valide
}
