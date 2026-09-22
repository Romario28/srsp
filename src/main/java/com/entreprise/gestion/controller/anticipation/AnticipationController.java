package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.service.anticipation.AlerteAnticipation;
import com.entreprise.gestion.service.anticipation.AnticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/anticipation")
@RequiredArgsConstructor
public class AnticipationController {

    private final AnticipationService anticipationService;

    // MODIFIÉ — "Integer horizonJours" → "Integer prevenanceJours, Integer retardJours"
    // (StatutAgent statut : paramètre déjà existant, conservé à l'identique)
    @GetMapping("/retraite")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> retraite(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.departsRetraite(prevenanceJours, retardJours, statut));
    }

    @GetMapping("/avancement")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> avancement(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.avancementsDus(prevenanceJours, retardJours, statut));
    }

    @GetMapping("/titularisation")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> titularisation(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.titularisationsDues(prevenanceJours, retardJours, statut));
    }

    @GetMapping("/fin-contrat")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> finContrat(
            @RequestParam(required = false) Integer prevenanceJours,
            @RequestParam(required = false) Integer retardJours,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.finsContrat(prevenanceJours, retardJours, statut));
    }

    // anomalies() — inchangée
    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> anomalies(
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.anomalies(statut));
    }
}
