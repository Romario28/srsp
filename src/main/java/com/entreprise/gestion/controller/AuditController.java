package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.AuditDTO;
import com.entreprise.gestion.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<List<AuditDTO>> getAll() {
        return ResponseEntity.ok(auditService.findAll());
    }
}