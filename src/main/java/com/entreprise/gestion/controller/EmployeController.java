package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.CreateEmployeRequest;
import com.entreprise.gestion.dto.EmployeResponse;
import com.entreprise.gestion.dto.UpdateEmployeRequest;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.EmployeService;
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

@RestController
@RequestMapping("/api/employes")
@RequiredArgsConstructor
public class EmployeController {

    private final EmployeService employeService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<Page<EmployeResponse>> getAll(
            @AuthenticationPrincipal UserDetailsImpl principal,
            @PageableDefault(size = 20, sort = "nom") Pageable pageable) {
        return ResponseEntity.ok(employeService.findAllVisibles(principal, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<EmployeResponse> getById(
            @PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(employeService.findByIdVisible(id, principal));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<EmployeResponse> create(
            @Valid @RequestBody CreateEmployeRequest req,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeService.create(req, principal));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<EmployeResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateEmployeRequest req,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(employeService.update(id, req, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl principal) {
        employeService.delete(id, principal);
        return ResponseEntity.noContent().build();
    }
}