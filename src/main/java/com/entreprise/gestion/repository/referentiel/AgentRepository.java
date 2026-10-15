package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.dto.anticipation.OptionFiltre;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.service.anticipation.CodesSituationAdministrative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AgentRepository extends JpaRepository<Agent, String> {

    /** Même définition que findAllActifs : sans situation, ou situation « en activité ». */
    String ACTIFS = "(a.sanction IS NULL OR a.sanction.code = '" + CodesSituationAdministrative.CODE_ACTIF + "')";

    // entityManager.clear() du batch (tous les 500 agents) rend illisibles : LazyInitializationException.
    @Query("SELECT a FROM Agent a " + "LEFT JOIN FETCH a.corps LEFT JOIN FETCH a.grade "
            + "WHERE a.sanction IS NULL OR a.sanction.code = '"
            + CodesSituationAdministrative.CODE_ACTIF + "'")
    List<Agent> findAllActifs();

    @Query("SELECT new com.entreprise.gestion.dto.anticipation.OptionFiltre(c.code, MIN(c.libelle)) "
            + "FROM Agent a JOIN a.corps c WHERE " + ACTIFS + " GROUP BY c.code ORDER BY c.code")
    List<OptionFiltre> optionsCorps();

    @Query("SELECT new com.entreprise.gestion.dto.anticipation.OptionFiltre(g.code, MIN(g.libelle)) "
            + "FROM Agent a JOIN a.grade g WHERE " + ACTIFS + " GROUP BY g.code ORDER BY g.code")
    List<OptionFiltre> optionsGrades();

    @Query("SELECT DISTINCT c.categorie FROM Agent a JOIN a.corps c WHERE " + ACTIFS + " ORDER BY c.categorie")
    List<String> categories();

    @Query("SELECT new com.entreprise.gestion.dto.anticipation.OptionFiltre(l.code, MIN(l.nom)) "
            + "FROM Agent a JOIN a.localite l WHERE " + ACTIFS + " GROUP BY l.code ORDER BY l.code")
    List<OptionFiltre> optionsFiv();

    @Query("SELECT new com.entreprise.gestion.dto.anticipation.OptionFiltre(s.code, MIN(s.libelle)) "
            + "FROM Agent a JOIN a.soa s WHERE " + ACTIFS + " GROUP BY s.code ORDER BY s.code")
    List<OptionFiltre> optionsSoa();

    @Query("SELECT new com.entreprise.gestion.dto.anticipation.OptionFiltre(m.code, MIN(m.libelle)) "
            + "FROM Agent a JOIN a.ministere m WHERE " + ACTIFS + " GROUP BY m.code ORDER BY m.code")
    List<OptionFiltre> optionsMinisteres();

    @Query("SELECT DISTINCT a.regCode FROM Agent a WHERE a.regCode IS NOT NULL AND " + ACTIFS + " ORDER BY a.regCode")
    List<String> regions();
}
