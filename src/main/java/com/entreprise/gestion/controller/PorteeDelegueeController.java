package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.CreatePorteeDelegueeRequest;
import com.entreprise.gestion.dto.PorteeDelegueeDTO;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.PorteeDelegueeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portees-deleguees")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
public class PorteeDelegueeController {

    private final PorteeDelegueeService porteeDelegueeService;

    @PostMapping
    public ResponseEntity<PorteeDelegueeDTO> accorder(
            @Valid @RequestBody CreatePorteeDelegueeRequest req,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(porteeDelegueeService.accorder(req, principal));
    }


    @GetMapping("/utilisateur/{idUtilisateur}")
    public ResponseEntity<List<PorteeDelegueeDTO>> getPourUtilisateur(@PathVariable Long idUtilisateur,
                                                                      @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(porteeDelegueeService.findPourUtilisateur(idUtilisateur, principal));
    }



    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoquer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        porteeDelegueeService.revoquer(id, principal);
        return ResponseEntity.noContent().build();
    }

}