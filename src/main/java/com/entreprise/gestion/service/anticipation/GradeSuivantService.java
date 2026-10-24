package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.config.anticipation.AnticipationProperties;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

/** Fournit l'index des grades ; gardé 30 s en mémoire car le moteur le consulte pour chaque agent. */
@Service
@RequiredArgsConstructor
public class GradeSuivantService {

    private static final long DUREE_CACHE_MS = 30_000;
    private record Instantane(GradesParCorps index, long chargeLe) {}

    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;
    private final AnticipationProperties proprietes;

    @Value("${anticipation.grades-stagiaire:ST0E}")
    private Set<String> gradesStagiaire;

    private volatile Instantane cache;

    public GradesParCorps indexer() {
        // après un import des indices, la prise en compte peut attendre 30 s
        Instantane courant = cache;
        long maintenant = System.currentTimeMillis();
        if (courant != null && maintenant - courant.chargeLe() < DUREE_CACHE_MS) return courant.index();
        GradesParCorps frais = new GradesParCorps(indiceGrdCorpsRepository.findAllLignes(), gradesStagiaire,
                proprietes.renommages());
        cache = new Instantane(frais, maintenant);
        return frais;
    }
}
