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
 * Consultation des échéances par type. Sans paramètre, chaque endpoint applique la
 * fenêtre configurée (base ou défaut) ; {@code prevenanceJours} / {@code retardJours}
 * permettent de la surcharger ponctuellement pour une requête.
 */
@RestController
@RequestMapping("/api/anticipation")
@RequiredArgsConstructor
public class AnticipationController {

    private final AnticipationService anticipationService;

    @GetMapping("/retraite")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> retraite(
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.departsRetraite(
                prevenance(horizonJours, prevenanceJours), retardJours, statut));
    }

    @GetMapping("/avancement")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> avancement(
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.avancementsDus(
                prevenance(horizonJours, prevenanceJours), retardJours, statut));
    }

    @GetMapping("/titularisation")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> titularisation(
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.titularisationsDues(
                prevenance(horizonJours, prevenanceJours), retardJours, statut));
    }

    @GetMapping("/fin-contrat")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> finContrat(
            @RequestParam(required = false) Integer horizonJours,
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.finsContrat(
                prevenance(horizonJours, prevenanceJours), retardJours, statut));
    }

    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> anomalies(
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.anomalies(statut));
    }

    /** {@code horizonJours} est l'ancien nom de la borne de prévenance : conservé pour compatibilité. */
    private Integer prevenance(Integer horizonJours, Integer prevenanceJours) {
        return prevenanceJours != null ? prevenanceJours : horizonJours;
    }
}
