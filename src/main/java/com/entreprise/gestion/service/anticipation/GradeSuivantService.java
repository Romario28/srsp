package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.config.anticipation.AnticipationProperties;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/** Charge l'index des grades une fois par calcul d'anticipation. */
@Service
@RequiredArgsConstructor
public class GradeSuivantService {

    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;
    private final AnticipationProperties proprietes;

    @Value("${anticipation.grades-stagiaire:ST0E}")
    private Set<String> gradesStagiaire;

    public GradesParCorps indexer() {
        return new GradesParCorps(indiceGrdCorpsRepository.findAllLignes(), gradesStagiaire,
                proprietes.renommages());
    }
}
