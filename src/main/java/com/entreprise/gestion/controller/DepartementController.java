package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.CreateDepartementRequest;
import com.entreprise.gestion.dto.DepartementResponse;
import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.service.DepartementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/departements")
@RequiredArgsConstructor
public class DepartementController {

    private final DepartementService departementService;

    @GetMapping
    public ResponseEntity<List<DepartementResponse>> getAll() {
        return ResponseEntity.ok(departementService.findAll().stream()
                .map(DepartementResponse::from).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DepartementResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(DepartementResponse.from(departementService.findEntityById(id)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartementResponse> create(@Valid @RequestBody CreateDepartementRequest req) {
        Departement d = departementService.create(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(DepartementResponse.from(d));
    }

    @PatchMapping("/{id}/chef")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartementResponse> definirChef(
            @PathVariable Long id, @RequestParam(required = false) Long idEmploye) {
        return ResponseEntity.ok(DepartementResponse.from(departementService.definirChef(id, idEmploye)));
    }

    @PatchMapping("/{id}/deplacer")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DepartementResponse> deplacer(
            @PathVariable Long id, @RequestParam(required = false) Long idNouveauParent) {
        return ResponseEntity.ok(DepartementResponse.from(departementService.deplacer(id, idNouveauParent)));
    }
}