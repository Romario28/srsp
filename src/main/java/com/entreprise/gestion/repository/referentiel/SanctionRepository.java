package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.entite.anticipation.Sanction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SanctionRepository extends JpaRepository<Sanction, String> {

}
