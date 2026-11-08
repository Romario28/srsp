package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.MonEspaceDTO;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.MonEspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mon-espace")
@RequiredArgsConstructor
@PreAuthorize("hasRole('AGENT')")
public class MonEspaceController {

    private final MonEspaceService monEspaceService;

    @GetMapping
    public ResponseEntity<MonEspaceDTO> consulter(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(monEspaceService.consulter(principal));
    }
}
