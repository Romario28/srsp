package com.entreprise.gestion.repository.referentiel;

import com.entreprise.gestion.entite.anticipation.IndiceGrdCorps;
import com.entreprise.gestion.entite.anticipation.IndiceGrdCorpsId;
import com.entreprise.gestion.service.anticipation.LigneIndice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IndiceGrdCorpsRepository extends JpaRepository<IndiceGrdCorps, IndiceGrdCorpsId> {

    @Query("SELECT new com.entreprise.gestion.service.anticipation.LigneIndice("
            + "i.corps.code, i.corps.categorie, i.grade.code, i.indice) FROM IndiceGrdCorps i")
    List<LigneIndice> findAllLignes();

}

