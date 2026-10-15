package com.entreprise.gestion.repository.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AlerteRepository extends JpaRepository<Alerte, Long> {

    List<Alerte> findByMatriculeAgentAndTypeAndStatutNot(String matricule, TypeAnticipation type, StatutAlerte statut);

    boolean existsByMatriculeAgentAndTypeAndStatutAndDateEcheance(
            String matricule, TypeAnticipation type, StatutAlerte statut, java.time.LocalDate dateEcheance);

    /** Partagée par la requête et son comptage : les deux ne peuvent pas diverger. */
    String CONDITIONS = """
            (:type IS NULL OR a.type = :type)
            AND (:statut IS NULL OR a.statut = :statut)
            AND (:matricule IS NULL OR a.matriculeAgent = :matricule)
            AND (:filtrerAgents = false OR EXISTS (
                SELECT 1 FROM Agent g
                LEFT JOIN g.corps c
                LEFT JOIN g.grade gr
                LEFT JOIN g.localite l
                LEFT JOIN g.soa s
                LEFT JOIN g.ministere m
                WHERE g.matricule = a.matriculeAgent
                  AND (:statutAgent IS NULL OR g.statut = :statutAgent)
                  AND (:corps IS NULL OR c.code = :corps)
                  AND (:grade IS NULL OR gr.code = :grade)
                  AND (:categorie IS NULL OR c.categorie = :categorie)
                  AND (:fiv IS NULL OR l.code = :fiv)
                  AND (:soa IS NULL OR s.code = :soa)
                  AND (:region IS NULL OR g.regCode = :region)
                  AND (:ministere IS NULL OR m.code = :ministere)))
            """;

    @Query(value = "SELECT a FROM Alerte a WHERE " + CONDITIONS + " ORDER BY a.dateEcheance ASC NULLS LAST",
            countQuery = "SELECT COUNT(a) FROM Alerte a WHERE " + CONDITIONS)
    Page<Alerte> rechercherFiltre(@Param("type") TypeAnticipation type,
                                  @Param("statut") StatutAlerte statut,
                                  @Param("matricule") String matricule,
                                  @Param("filtrerAgents") boolean filtrerAgents,
                                  @Param("statutAgent") StatutAgent statutAgent,
                                  @Param("corps") String corps,
                                  @Param("grade") String grade,
                                  @Param("categorie") String categorie,
                                  @Param("fiv") String fiv,
                                  @Param("soa") String soa,
                                  @Param("region") String region,
                                  @Param("ministere") String ministere,
                                  Pageable page);

    default Page<Alerte> rechercher(TypeAnticipation type, StatutAlerte statut, String matricule,
                                    FiltreAgents f, Pageable page) {
        return rechercherFiltre(type, statut, matricule, !f.estVide(), f.statutAgent(), f.corps(), f.grade(),
                f.categorie(), f.fiv(), f.soa(), f.region(), f.ministere(), page);
    }

    // Toutes les alertes d'un agent pour un type, tous statuts confondus : sert à dédupliquer les anomalies par raison.
    List<Alerte> findByMatriculeAgentAndType(String matricule, TypeAnticipation type);
}
