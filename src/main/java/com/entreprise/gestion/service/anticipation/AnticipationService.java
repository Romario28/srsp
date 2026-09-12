// AnticipationService.java
package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnticipationService {

    private final AgentRepository agentRepository;
    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> departsRetraite(int seuilAnnees) {
        LocalDate limite = LocalDate.now().minusYears(seuilAnnees);
        return agentRepository.findByStatutAndDateNaissanceLessThanEqual(StatutAgent.FONCTIONNAIRE, limite)
                .stream()
                .filter(this::estEnActivite)
                .map(a -> alerteRetraite(a, seuilAnnees))
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlerteAnticipation> avancementsDus(int horizonJours) {
        return agentRepository.findAllWithCorpsGradeForAvancement().stream()
                .filter(this::estEnActivite)
                .map(this::alerteAvancement)
                .filter(Objects::nonNull)
                .filter(al -> al.joursRestants() <= horizonJours)
                .sorted(Comparator.comparingLong(AlerteAnticipation::joursRestants))
                .collect(Collectors.toList());
    }

    private boolean estEnActivite(Agent a) {
        return a.getSanction() == null
                || !CodesSituationAdministrative.SORTIE_DEFINITIVE.contains(a.getSanction().getCode());
    }

    private AlerteAnticipation alerteRetraite(Agent a, int seuil) {
        LocalDate echeance = a.getDateNaissance().plusYears(seuil);
        long jours = ChronoUnit.DAYS.between(LocalDate.now(), echeance);
        return new AlerteAnticipation(a.getMatricule(), a.getPrenoms() + " " + a.getNom(),
                TypeAnticipation.DEPART_RETRAITE, echeance, jours,
                jours < 0 ? "Dépassé de " + Math.abs(jours) / 365 + " an(s)" : null);
    }

    private AlerteAnticipation alerteAvancement(Agent a) {
        if (a.getAvanceDate() == null || a.getCorps() == null || a.getGrade() == null) return null;

        IndiceGrdCorps ref = indiceGrdCorpsRepository
                .findById(new IndiceGrdCorpsId(
                        a.getGrade().getCode(),
                        new CorpsId(a.getCorps().getCode(), a.getCorps().getCategorie())))
                .orElse(null);
        if (ref == null || ref.getDureeRequise() == null) return null;

        LocalDate echeance = a.getAvanceDate().plusMonths(ref.getDureeRequise());
        long jours = ChronoUnit.DAYS.between(LocalDate.now(), echeance);
        return new AlerteAnticipation(a.getMatricule(), a.getPrenoms() + " " + a.getNom(),
                TypeAnticipation.AVANCEMENT, echeance, jours, null);
    }
}