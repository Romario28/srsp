// repository/anticipation/ConfigurationDelaiRepository.java
package com.entreprise.gestion.repository.anticipation;

import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ConfigurationDelaiRepository extends JpaRepository<ConfigurationDelai, TypeAnticipation> {
    List<ConfigurationDelai> findByActifTrue();
}