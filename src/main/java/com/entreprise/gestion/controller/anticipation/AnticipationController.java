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
            @RequestParam(defaultValue = "60") int seuilAnnees) {
        return ResponseEntity.ok(anticipationService.departsRetraite(seuilAnnees));
    }

    @GetMapping("/avancement")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<AlerteAnticipation>> avancement(
            @RequestParam(defaultValue = "90") int horizonJours) {
        return ResponseEntity.ok(anticipationService.avancementsDus(horizonJours));
    }
}