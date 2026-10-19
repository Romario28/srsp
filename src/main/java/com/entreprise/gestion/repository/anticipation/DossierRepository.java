package com.entreprise.gestion.repository.anticipation;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import com.entreprise.gestion.service.dossier.CleDossier;
import com.entreprise.gestion.service.dossier.VueDossier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface DossierRepository extends JpaRepository<Dossier,Long> {
    String EN_ATTENTE="com.entreprise.gestion.entite.anticipation.StatutDossier.EN_ATTENTE_DE_TRAITEMENT";
    String EN_COURS="com.entreprise.gestion.entite.anticipation.StatutDossier.EN_COURS";
    String DERNIERE_ETAPE="COALESCE(d.dateFin,d.dateSolde,d.datePersTana,d.datePrefet,d.dateCf,d.dateVisa,d.dateFop,d.dateChefAvantFop)";
    String JOINTURES="JOIN d.agent g LEFT JOIN g.corps c LEFT JOIN g.grade gr LEFT JOIN g.localite l LEFT JOIN g.soa s LEFT JOIN g.ministere m";
    String JOINTURES_FETCH="JOIN FETCH d.agent g LEFT JOIN FETCH g.corps c LEFT JOIN FETCH g.grade gr LEFT JOIN FETCH g.hee LEFT JOIN FETCH g.sanction LEFT JOIN FETCH g.localite l LEFT JOIN FETCH g.soa s LEFT JOIN FETCH g.ministere m";
    String FILTRES="d.statut IN :statuts AND (:type IS NULL OR d.type=:type) AND (:matricule IS NULL OR g.matricule=:matricule) "
            +"AND (:preparationMax IS NULL OR d.datePreparation<=:preparationMax) AND (:echeanceMin IS NULL OR d.dateEcheance>=:echeanceMin) "
            +"AND (:echeanceMax IS NULL OR d.dateEcheance<=:echeanceMax) AND (:statutAgent IS NULL OR g.statut=:statutAgent) "
            +"AND (:corps IS NULL OR c.code=:corps) AND (:grade IS NULL OR gr.code=:grade) AND (:categorie IS NULL OR c.categorie=:categorie) "
            +"AND (:fiv IS NULL OR l.code=:fiv) AND (:soa IS NULL OR s.code=:soa) AND (:region IS NULL OR g.regCode=:region) AND (:ministere IS NULL OR m.code=:ministere)";
    @Query(value="SELECT d FROM Dossier d "+JOINTURES_FETCH+" WHERE "+FILTRES,
            countQuery="SELECT COUNT(d) FROM Dossier d "+JOINTURES+" WHERE "+FILTRES)
    Page<Dossier> rechercherFiltre(@Param("statuts") Collection<StatutDossier> statuts,@Param("type") TypeAnticipation type,
                                   @Param("matricule") String matricule,@Param("preparationMax") LocalDate preparationMax,
                                   @Param("echeanceMin") LocalDate echeanceMin,@Param("echeanceMax") LocalDate echeanceMax,
                                   @Param("statutAgent") StatutAgent statutAgent,@Param("corps") String corps,@Param("grade") String grade,
                                   @Param("categorie") String categorie,@Param("fiv") String fiv,@Param("soa") String soa,
                                   @Param("region") String region,@Param("ministere") String ministere,Pageable page);
    default Page<Dossier> rechercher(Collection<StatutDossier> statuts,TypeAnticipation type,String matricule,
                                     VueDossier.Bornes b,FiltreAgents f,Pageable page) {
        return rechercherFiltre(statuts,type,matricule,b.preparationMax(),b.echeanceMin(),b.echeanceMax(),
                f.statutAgent(),f.corps(),f.grade(),f.categorie(),f.fiv(),f.soa(),f.region(),f.ministere(),page);
    }
    @Query("SELECT d FROM Dossier d JOIN FETCH d.agent WHERE d.id IN :ids")
    List<Dossier> avecAgents(@Param("ids") Collection<Long> ids);
    @Query("SELECT new com.entreprise.gestion.service.dossier.CleDossier(d.agent.matricule,d.type,d.dateEcheance) FROM Dossier d")
    List<CleDossier> cles();
    long countByStatut(StatutDossier statut);
    @Query("SELECT COUNT(d) FROM Dossier d WHERE d.statut = "+EN_ATTENTE+" AND d.datePreparation <= :aujourdhui AND d.dateEcheance >= :aujourdhui")
    long compterATraiter(@Param("aujourdhui") LocalDate aujourdhui);
    @Query("SELECT COUNT(d) FROM Dossier d WHERE d.statut = "+EN_ATTENTE+" AND d.dateEcheance < :aujourdhui")
    long compterEnRetard(@Param("aujourdhui") LocalDate aujourdhui);
    @Query("SELECT COUNT(d) FROM Dossier d WHERE d.statut = "+EN_COURS+" AND "+DERNIERE_ETAPE+" < :seuil")
    long compterARelancer(@Param("seuil") LocalDate seuil);
}
