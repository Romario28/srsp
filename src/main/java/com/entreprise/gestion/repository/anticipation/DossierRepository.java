package com.entreprise.gestion.repository.anticipation;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.acces.Perimetre;
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

    // PostgreSQL ne peut pas typer un « ? is null » : chaque test de nullité est précédé d'un cast.
    // Les énumérations (type, statutAgent) sont reçues sous forme de texte et comparées à cast(colonne as string).
    String FILTRES_COMMUNS="(cast(:type as string) IS NULL OR cast(d.type as string)=:type) "
            +"AND (cast(:matricule as string) IS NULL OR g.matricule=:matricule) "
            +"AND (cast(:statutAgent as string) IS NULL OR cast(g.statut as string)=:statutAgent) "
            +"AND (cast(:corps as string) IS NULL OR c.code=:corps) AND (cast(:grade as string) IS NULL OR gr.code=:grade) "
            +"AND (cast(:categorie as string) IS NULL OR c.categorie=:categorie) AND (cast(:fiv as string) IS NULL OR l.code=:fiv) "
            +"AND (cast(:soa as string) IS NULL OR s.code=:soa) "
            +"AND (cast(:region as string) IS NULL OR g.regCode=:region) AND (cast(:ministere as string) IS NULL OR m.code=:ministere) "
            +"AND (:pMinistere = false OR m.code IN :pMinisteres) AND (:pSoa = false OR s.code IN :pSoas) AND (:pFiv = false OR l.code IN :pFivs) "
            +"AND (:multiples = false OR (SELECT COUNT(x) FROM Dossier x WHERE x.agent.matricule = g.matricule) > 1)";
    String FILTRES="d.statut IN :statuts AND (cast(:preparationMax as LocalDate) IS NULL OR d.datePreparation<=:preparationMax) "
            +"AND (cast(:echeanceMin as LocalDate) IS NULL OR d.dateEcheance>=:echeanceMin) "
            +"AND (cast(:echeanceMax as LocalDate) IS NULL OR d.dateEcheance<=:echeanceMax) AND "+FILTRES_COMMUNS;

    @Query(value="SELECT d FROM Dossier d "+JOINTURES_FETCH+" WHERE "+FILTRES,
            countQuery="SELECT COUNT(d) FROM Dossier d "+JOINTURES+" WHERE "+FILTRES)
    Page<Dossier> rechercherFiltre(@Param("statuts") Collection<StatutDossier> statuts,@Param("preparationMax") LocalDate preparationMax,
            @Param("echeanceMin") LocalDate echeanceMin,@Param("echeanceMax") LocalDate echeanceMax,
            @Param("type") String type,@Param("matricule") String matricule,@Param("statutAgent") String statutAgent,
            @Param("corps") String corps,@Param("grade") String grade,@Param("categorie") String categorie,
            @Param("fiv") String fiv,@Param("soa") String soa,@Param("region") String region,@Param("ministere") String ministere,
            @Param("multiples") boolean multiples,
            @Param("pMinistere") boolean pMinistere,@Param("pMinisteres") Collection<String> pMinisteres,
            @Param("pSoa") boolean pSoa,@Param("pSoas") Collection<String> pSoas,
            @Param("pFiv") boolean pFiv,@Param("pFivs") Collection<String> pFivs,
            Pageable page);

    default Page<Dossier> rechercher(Collection<StatutDossier> statuts,TypeAnticipation type,String matricule,VueDossier.Bornes b,
            FiltreAgents f,boolean multiples,Perimetre p,Pageable page) {
        if (p.estVide()) return Page.empty(page);
        return rechercherFiltre(statuts,b.preparationMax(),b.echeanceMin(),b.echeanceMax(),nom(type),matricule,
                nom(f.statutAgent()),f.corps(),f.grade(),f.categorie(),f.fiv(),f.soa(),f.region(),f.ministere(),multiples,
                p.restreintMinisteres(),p.ministeresPourRequete(),p.restreintSoas(),p.soasPourRequete(),
                p.restreintFivs(),p.fivsPourRequete(),page);
    }

    @Query("SELECT d FROM Dossier d "+JOINTURES_FETCH+" WHERE "+FILTRES)
    List<Dossier> rechercherTousFiltre(@Param("statuts") Collection<StatutDossier> statuts,@Param("preparationMax") LocalDate preparationMax,
            @Param("echeanceMin") LocalDate echeanceMin,@Param("echeanceMax") LocalDate echeanceMax,
            @Param("type") String type,@Param("matricule") String matricule,@Param("statutAgent") String statutAgent,
            @Param("corps") String corps,@Param("grade") String grade,@Param("categorie") String categorie,
            @Param("fiv") String fiv,@Param("soa") String soa,@Param("region") String region,@Param("ministere") String ministere,
            @Param("multiples") boolean multiples,
            @Param("pMinistere") boolean pMinistere,@Param("pMinisteres") Collection<String> pMinisteres,
            @Param("pSoa") boolean pSoa,@Param("pSoas") Collection<String> pSoas,
            @Param("pFiv") boolean pFiv,@Param("pFivs") Collection<String> pFivs,
            Sort sort);

    default List<Dossier> rechercherTous(Collection<StatutDossier> statuts,TypeAnticipation type,String matricule,VueDossier.Bornes b,
            FiltreAgents f,boolean multiples,Perimetre p,Sort sort) {
        if (p.estVide()) return List.of();
        return rechercherTousFiltre(statuts,b.preparationMax(),b.echeanceMin(),b.echeanceMax(),nom(type),matricule,
                nom(f.statutAgent()),f.corps(),f.grade(),f.categorie(),f.fiv(),f.soa(),f.region(),f.ministere(),multiples,
                p.restreintMinisteres(),p.ministeresPourRequete(),p.restreintSoas(),p.soasPourRequete(),
                p.restreintFivs(),p.fivsPourRequete(),sort);
    }

    Optional<Dossier> findByAgentMatriculeAndTypeAndDateEcheance(String matricule,TypeAnticipation type,LocalDate dateEcheance);

    List<Dossier> findByAgentMatriculeOrderByDateEcheanceDesc(String matricule);

    @Query("SELECT COUNT(d) FROM Dossier d "+JOINTURES+" WHERE "+FILTRES)
    long compterFiltre(@Param("statuts") Collection<StatutDossier> statuts,@Param("preparationMax") LocalDate preparationMax,
            @Param("echeanceMin") LocalDate echeanceMin,@Param("echeanceMax") LocalDate echeanceMax,
            @Param("type") String type,@Param("matricule") String matricule,@Param("statutAgent") String statutAgent,
            @Param("corps") String corps,@Param("grade") String grade,@Param("categorie") String categorie,
            @Param("fiv") String fiv,@Param("soa") String soa,@Param("region") String region,@Param("ministere") String ministere,
            @Param("multiples") boolean multiples,
            @Param("pMinistere") boolean pMinistere,@Param("pMinisteres") Collection<String> pMinisteres,
            @Param("pSoa") boolean pSoa,@Param("pSoas") Collection<String> pSoas,
            @Param("pFiv") boolean pFiv,@Param("pFivs") Collection<String> pFivs);

    default long compter(Collection<StatutDossier> statuts,TypeAnticipation type,String matricule,VueDossier.Bornes b,
            FiltreAgents f,boolean multiples,Perimetre p) {
        if (p.estVide()) return 0;
        return compterFiltre(statuts,b.preparationMax(),b.echeanceMin(),b.echeanceMax(),nom(type),matricule,
                nom(f.statutAgent()),f.corps(),f.grade(),f.categorie(),f.fiv(),f.soa(),f.region(),f.ministere(),multiples,
                p.restreintMinisteres(),p.ministeresPourRequete(),p.restreintSoas(),p.soasPourRequete(),
                p.restreintFivs(),p.fivsPourRequete());
    }

    /** Nom d'une énumération pour les requêtes ci-dessus ; null si absente. */
    private static String nom(Enum<?> valeur) { return valeur == null ? null : valeur.name(); }

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
