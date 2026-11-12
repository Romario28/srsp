package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.DepartementDTO;
import com.entreprise.gestion.dto.DepartementRequest;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.DepartementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/departements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class DepartementController {

    private final DepartementService departementService;

    @GetMapping
    public ResponseEntity<List<DepartementDTO>> lister() {
        return ResponseEntity.ok(departementService.lister());
    }

    @PostMapping
    public ResponseEntity<DepartementDTO> creer(@Valid @RequestBody DepartementRequest req,
                                                @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(departementService.creer(req.libelle(), principal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DepartementDTO> modifier(@PathVariable Long id, @Valid @RequestBody DepartementRequest req,
                                                   @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(departementService.modifier(id, req.libelle(), principal));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> supprimer(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl principal) {
        departementService.supprimer(id, principal);
        return ResponseEntity.noContent().build();
    }
}
