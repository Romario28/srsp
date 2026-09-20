// AnticipationController.java
package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.service.anticipation.AlerteAnticipation;
import com.entreprise.gestion.service.anticipation.AnticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

/**
 * Échéances à anticiper, filtrées par la fenêtre du type considéré :
 * configurée en base si une surcharge active existe, sinon la fenêtre par défaut
 * (voir ConfigurationDelaiService — même résolution que le batch nocturne).
 *
 * Sans paramètre, on obtient exactement la fenêtre effective du type.
 * Les paramètres {@code prevenanceJours} / {@code retardJours} permettent un
 * ajustement ponctuel de l'une des deux bornes, sans toucher à la configuration.
 * {@code horizonJours} reste accepté comme alias de {@code prevenanceJours}.
 */
@RestController
@RequestMapping("/api/anticipation")
@RequiredArgsConstructor
public class AnticipationController {

    private final AnticipationService anticipationService;

    @GetMapping("/retraite")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> retraite(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.departsRetraite(
                prevenance(prevenanceJours, horizonJours), retardJours, statut));
    }

    @GetMapping("/avancement")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> avancement(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.avancementsDus(
                prevenance(prevenanceJours, horizonJours), retardJours, statut));
    }

    @GetMapping("/titularisation")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> titularisation(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.titularisationsDues(
                prevenance(prevenanceJours, horizonJours), retardJours, statut));
    }

    @GetMapping("/fin-contrat")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> finContrat(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.finsContrat(
                prevenance(prevenanceJours, horizonJours), retardJours, statut));
    }

    /** Les anomalies n'ont pas de fenêtre : elles sont toujours remontées. */
    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> anomalies(
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.anomalies(statut));
    }

    private static Integer prevenance(Integer prevenanceJours, Integer horizonJours) {
        return prevenanceJours != null ? prevenanceJours : horizonJours;
    }
}
