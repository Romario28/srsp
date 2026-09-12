package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.entite.anticipation.Corps;
import com.entreprise.gestion.entite.anticipation.CorpsId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorpsRepository extends JpaRepository<Corps, CorpsId> {


    /** Utilisé par l'import pour dédupliquer les triplets (corps,categorie,libelle) réellement identiques. */
//    Optional<Corps> findByCorpsCodeAndCategorieAndLibelle(String corpsCode, String categorie, String libelle);
}
