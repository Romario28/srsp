package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.ApprobationRequest;
import com.entreprise.gestion.dto.DemandeCompteDTO;
import com.entreprise.gestion.dto.RefusRequest;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.DemandeCompteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demandes-comptes")
@RequiredArgsConstructor
public class DemandeCompteController {

    private final DemandeCompteService demandeCompteService;

    @GetMapping
    public ResponseEntity<List<DemandeCompteDTO>> lister(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(demandeCompteService.lister(principal));
    }

    @GetMapping("/compteur")
    public ResponseEntity<Long> compteur(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(demandeCompteService.compter(principal));
    }

    @PostMapping("/{id}/approbation")
    public ResponseEntity<Void> approuver(@PathVariable Long id,
                                          @RequestBody(required = false) ApprobationRequest req,
                                          @AuthenticationPrincipal UserDetailsImpl principal) {
        demandeCompteService.approuver(id, req != null ? req : new ApprobationRequest(null), principal);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/refus")
    public ResponseEntity<Void> refuser(@PathVariable Long id,
                                        @Valid @RequestBody(required = false) RefusRequest req,
                                        @AuthenticationPrincipal UserDetailsImpl principal) {
        demandeCompteService.refuser(id, req != null ? req : new RefusRequest(null), principal);
        return ResponseEntity.ok().build();
    }
}
