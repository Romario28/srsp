package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.CreateUtilisateurRequest;
import com.entreprise.gestion.dto.UtilisateurDTO;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.UtilisateurService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/utilisateurs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class UtilisateurController {

    private final UtilisateurService utilisateurService;

    @GetMapping
    public ResponseEntity<Page<UtilisateurDTO>> getAll(
            @PageableDefault(size = 20, sort = "email") Pageable pageable) {
        return ResponseEntity.ok(utilisateurService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.findById(id));
    }

    @PostMapping
    public ResponseEntity<?> create(
            @Valid @RequestBody CreateUtilisateurRequest req,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        try {
            Utilisateur u = utilisateurService.create(req, principal);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id",     u.getId(),
                    "email",  u.getEmail(),
                    "statut", u.getStatut().name()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    
    @PatchMapping("/{id}/statut")
    public ResponseEntity<?> changeStatut(
            @PathVariable Long id,
            @RequestParam String statut,
            @AuthenticationPrincipal UserDetailsImpl principal) {

        Utilisateur u = utilisateurService.changeStatut(id, statut, principal);
        // Plus de try/catch : BusinessException remonte vers GlobalExceptionHandler
        // qui renvoie { "code": "DERNIER_ADMIN", "message": "..." } en 400
        return ResponseEntity.ok(Map.of(
                "id",     u.getId(),
                "email",  u.getEmail(),
                "statut", u.getStatut().name()
        ));
    }
}
