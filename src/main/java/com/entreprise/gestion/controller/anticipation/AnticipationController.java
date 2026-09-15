// AnticipationController.java
package com.entreprise.gestion.controller.anticipation;

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

    @GetMapping("/retraite")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> retraite(
            @RequestParam(required = false) Integer horizonJours) {
        return ResponseEntity.ok(anticipationService.departsRetraite(horizonJours));
    }

    @GetMapping("/avancement")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> avancement(
            @RequestParam(required = false) Integer horizonJours) {
        return ResponseEntity.ok(anticipationService.avancementsDus(horizonJours));
    }

    @GetMapping("/titularisation")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> titularisation(
            @RequestParam(required = false) Integer horizonJours) {
        return ResponseEntity.ok(anticipationService.titularisationsDues(horizonJours));
    }

    @GetMapping("/fin-contrat")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> finContrat(
            @RequestParam(required = false) Integer horizonJours) {
        return ResponseEntity.ok(anticipationService.finsContrat(horizonJours));
    }

    @GetMapping("/anomalies")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> anomalies() {
        return ResponseEntity.ok(anticipationService.anomalies());
    }
}