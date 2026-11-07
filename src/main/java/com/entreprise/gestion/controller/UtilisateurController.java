package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.*;
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
    public ResponseEntity<Page<UtilisateurDTO>> getAll(@PageableDefault(size = 20, sort = "email") Pageable pageable) {
        return ResponseEntity.ok(utilisateurService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UtilisateurDetailDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(utilisateurService.findById(id));
    }

    // [NOUVEAU] aperçu de l'agent d'un matricule saisi (formulaire de compte)
    @GetMapping("/agents/{matricule}")
    public ResponseEntity<AgentLieDTO> apercuAgent(@PathVariable String matricule) {
        return ResponseEntity.ok(utilisateurService.apercuAgent(matricule));
    }

    @PostMapping
    public ResponseEntity<UtilisateurDetailDTO> create(@Valid @RequestBody CreateUtilisateurRequest req,
                                                       @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(utilisateurService.create(req, principal));
    }

    // [NOUVEAU]
    @PutMapping("/{id}")
    public ResponseEntity<UtilisateurDetailDTO> update(@PathVariable Long id, @Valid @RequestBody UpdateUtilisateurRequest req,
                                                       @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(utilisateurService.update(id, req, principal));
    }

    @PatchMapping("/{id}/statut")
    public ResponseEntity<?> changeStatut(@PathVariable Long id, @RequestParam String statut,
                                          @AuthenticationPrincipal UserDetailsImpl principal) {
        Utilisateur u = utilisateurService.changeStatut(id, statut, principal);
        return ResponseEntity.ok(Map.of("id", u.getId(), "email", u.getEmail(), "statut", u.getStatut().name()));
    }
}
