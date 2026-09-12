
package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.anticipation.AlerteService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/alertes")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
public class AlerteController {

    private final AlerteService alerteService;

    @GetMapping
    public ResponseEntity<Page<Alerte>> lister(
            @RequestParam(required = false) TypeAnticipation type,
            @RequestParam(required = false) StatutAlerte statut,
            @RequestParam(required = false) String matricule,
            Pageable page) {
        return ResponseEntity.ok(alerteService.lister(type, statut, matricule, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Alerte> consulter(@PathVariable Long id) {
        return ResponseEntity.ok(alerteService.consulter(id));
    }

    @PatchMapping("/{id}/acquitter")
    public ResponseEntity<Alerte> acquitter(@PathVariable Long id,
                                            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(alerteService.acquitter(id, principal));
    }
}