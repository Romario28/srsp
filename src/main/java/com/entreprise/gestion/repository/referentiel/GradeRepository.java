package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.entite.anticipation.Grade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GradeRepository extends JpaRepository<Grade, String> {
}
