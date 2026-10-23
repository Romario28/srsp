package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.dashboard.EcheancesAVenirDTO;
import com.entreprise.gestion.dto.dashboard.EffectifsDTO;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import com.entreprise.gestion.service.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/effectifs")
    public ResponseEntity<EffectifsDTO> effectifs(@ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(dashboardService.effectifs(filtre));
    }

    @GetMapping("/echeances")
    public ResponseEntity<EcheancesAVenirDTO> echeances(@RequestParam(defaultValue = "30") int jours,
                                                        @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(dashboardService.echeances(jours, filtre));
    }
}
