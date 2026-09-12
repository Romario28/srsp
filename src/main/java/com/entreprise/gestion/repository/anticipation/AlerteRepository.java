// repository/anticipation/AlerteRepository.java
package com.entreprise.gestion.repository.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
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

    @Query("SELECT a FROM Alerte a WHERE " +
            "(:type IS NULL OR a.type = :type) AND " +
            "(:statut IS NULL OR a.statut = :statut) AND " +
            "(:matricule IS NULL OR a.matriculeAgent = :matricule) " +
            "ORDER BY a.dateEcheance ASC NULLS LAST")
    Page<Alerte> rechercher(@Param("type") TypeAnticipation type,
                            @Param("statut") StatutAlerte statut,
                            @Param("matricule") String matricule,
                            Pageable page);
}