package com.entreprise.gestion.repository.anticipation;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import com.entreprise.gestion.service.dossier.CleDossier;
import com.entreprise.gestion.service.dossier.DossierResume;
import com.entreprise.gestion.service.dossier.NbDossiersAgent;
import com.entreprise.gestion.service.dossier.VueDossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DossierRepository extends JpaRepository<Dossier,Long> {
    String JOINTURES="JOIN d.agent g LEFT JOIN g.corps c LEFT JOIN g.grade gr LEFT JOIN g.localite l LEFT JOIN g.soa s LEFT JOIN g.ministere m";
    String JOINTURES_FETCH="JOIN FETCH d.agent g LEFT JOIN FETCH g.corps c LEFT JOIN FETCH g.grade gr LEFT JOIN FETCH g.hee LEFT JOIN FETCH g.sanction LEFT JOIN FETCH g.localite l LEFT JOIN FETCH g.soa s LEFT JOIN FETCH g.ministere m";
    String FILTRES_COMMUNS="(:type IS NULL OR d.type=:type) AND (:matricule IS NULL OR g.matricule=:matricule) "
            +"AND (:statutAgent IS NULL OR g.statut=:statutAgent) AND (:corps IS NULL OR c.code=:corps) AND (:grade IS NULL OR gr.code=:grade) "
            +"AND (:categorie IS NULL OR c.categorie=:categorie) AND (:fiv IS NULL OR l.code=:fiv) AND (:soa IS NULL OR s.code=:soa) "
            +"AND (:region IS NULL OR g.regCode=:region) AND (:ministere IS NULL OR m.code=:ministere) "
            // agents ayant au moins deux dossiers (tous états confondus)
            +"AND (:multiples = false OR (SELECT COUNT(x) FROM Dossier x WHERE x.agent.matricule = g.matricule) > 1)";
    String FILTRES="d.statut IN :statuts AND (:preparationMax IS NULL OR d.datePreparation<=:preparationMax) "
            +"AND (:echeanceMin IS NULL OR d.dateEcheance>=:echeanceMin) AND (:echeanceMax IS NULL OR d.dateEcheance<=:echeanceMax) AND "+FILTRES_COMMUNS;

    @Query(value="SELECT d FROM Dossier d "+JOINTURES_FETCH+" WHERE "+FILTRES,
            countQuery="SELECT COUNT(d) FROM Dossier d "+JOINTURES+" WHERE "+FILTRES)
    Page<Dossier> rechercherFiltre(@Param("statuts") Collection<StatutDossier> statuts,@Param("preparationMax") LocalDate preparationMax,
            @Param("echeanceMin") LocalDate echeanceMin,@Param("echeanceMax") LocalDate echeanceMax,
            @Param("type") TypeAnticipation type,@Param("matricule") String matricule,@Param("statutAgent") StatutAgent statutAgent,
            @Param("corps") String corps,@Param("grade") String grade,@Param("categorie") String categorie,
            @Param("fiv") String fiv,@Param("soa") String soa,@Param("region") String region,@Param("ministere") String ministere,
            @Param("multiples") boolean multiples,Pageable page);

    default Page<Dossier> rechercher(Collection<StatutDossier> statuts,TypeAnticipation type,String matricule,VueDossier.Bornes b,
            FiltreAgents f,boolean multiples,Pageable page) {
        return rechercherFiltre(statuts,b.preparationMax(),b.echeanceMin(),b.echeanceMax(),type,matricule,
                f.statutAgent(),f.corps(),f.grade(),f.categorie(),f.fiv(),f.soa(),f.region(),f.ministere(),multiples,page);
    }

    @Query("SELECT d FROM Dossier d "+JOINTURES_FETCH+" WHERE "+FILTRES)
    List<Dossier> rechercherTousFiltre(@Param("statuts") Collection<StatutDossier> statuts,@Param("preparationMax") LocalDate preparationMax,
            @Param("echeanceMin") LocalDate echeanceMin,@Param("echeanceMax") LocalDate echeanceMax,
            @Param("type") TypeAnticipation type,@Param("matricule") String matricule,@Param("statutAgent") StatutAgent statutAgent,
            @Param("corps") String corps,@Param("grade") String grade,@Param("categorie") String categorie,
            @Param("fiv") String fiv,@Param("soa") String soa,@Param("region") String region,@Param("ministere") String ministere,
            @Param("multiples") boolean multiples, Sort sort);

    default List<Dossier> rechercherTous(Collection<StatutDossier> statuts,TypeAnticipation type,String matricule,VueDossier.Bornes b,
            FiltreAgents f,boolean multiples,Sort sort) {
        return rechercherTousFiltre(statuts,b.preparationMax(),b.echeanceMin(),b.echeanceMax(),type,matricule,
                f.statutAgent(),f.corps(),f.grade(),f.categorie(),f.fiv(),f.soa(),f.region(),f.ministere(),multiples,sort);
    }

    Optional<Dossier> findByAgentMatriculeAndTypeAndDateEcheance(String matricule,TypeAnticipation type,LocalDate dateEcheance);

    @Query("SELECT COUNT(d) FROM Dossier d "+JOINTURES+" WHERE "+FILTRES)
    long compterFiltre(@Param("statuts") Collection<StatutDossier> statuts,@Param("preparationMax") LocalDate preparationMax,
            @Param("echeanceMin") LocalDate echeanceMin,@Param("echeanceMax") LocalDate echeanceMax,
            @Param("type") TypeAnticipation type,@Param("matricule") String matricule,@Param("statutAgent") StatutAgent statutAgent,
            @Param("corps") String corps,@Param("grade") String grade,@Param("categorie") String categorie,
            @Param("fiv") String fiv,@Param("soa") String soa,@Param("region") String region,@Param("ministere") String ministere,
            @Param("multiples") boolean multiples);
    default long compter(Collection<StatutDossier> statuts,TypeAnticipation type,String matricule,VueDossier.Bornes b,
            FiltreAgents f,boolean multiples) {
        return compterFiltre(statuts,b.preparationMax(),b.echeanceMin(),b.echeanceMax(),type,matricule,
                f.statutAgent(),f.corps(),f.grade(),f.categorie(),f.fiv(),f.soa(),f.region(),f.ministere(),multiples);
    }

    // total des dossiers d'un agent (une ligne) et pour une page entière (une requête)
    long countByAgentMatricule(String matricule);

    @Query("SELECT new com.entreprise.gestion.service.dossier.NbDossiersAgent(d.agent.matricule, COUNT(d)) "
            +"FROM Dossier d WHERE d.agent.matricule IN :matricules GROUP BY d.agent.matricule")
    List<NbDossiersAgent> compterParAgent(@Param("matricules") Collection<String> matricules);

    @Query("SELECT new com.entreprise.gestion.service.dossier.CleDossier(d.agent.matricule,d.type,d.dateEcheance) FROM Dossier d")
    List<CleDossier> cles();

    String RESUME="SELECT new com.entreprise.gestion.service.dossier.DossierResume(d.agent.matricule,d.type,d.dateEcheance,d.statut,"
            +"d.dateDemarrage,d.motifSansSuite) FROM Dossier d";
    @Query(RESUME) List<DossierResume> resumes();
    @Query(RESUME+" WHERE d.agent.matricule IN :matricules")
    List<DossierResume> resumesDe(@Param("matricules") Collection<String> matricules);
}
