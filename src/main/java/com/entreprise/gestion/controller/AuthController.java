package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.LoginRequest;
import com.entreprise.gestion.dto.LoginResponse;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest http) {
        try {
            LoginResponse resp = authService.login(req, http.getRemoteAddr());
            return ResponseEntity.ok(resp);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email ou mot de passe incorrect"));
        } catch (DisabledException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Compte désactivé ou suspendu"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Erreur serveur : " + e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            HttpServletRequest req,
            @AuthenticationPrincipal UserDetailsImpl principal) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            authService.logout(header.substring(7), principal.getEmail());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/auth/me
     *
     * AVANT : auth.getName() pour l'email, pas d'accès à l'entité.
     * APRÈS : @AuthenticationPrincipal donne tout — id, nomComplet, groupe,
     *         employé, statut — sans aucune requête BDD.
     */
   /* @GetMapping("/me")
    public ResponseEntity<?> me(
            @AuthenticationPrincipal UserDetailsImpl principal) {

        return ResponseEntity.ok(Map.of(
                "id",                   principal.getId(),
                "email",                principal.getEmail(),
                "nomComplet",           principal.getNomComplet(),
                "statut",               principal.getStatut().name(),
                "nomGroupe",            principal.getNomGroupe() != null ? principal.getNomGroupe() : "",
                "roles",                principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toList()),
                "dateDerniereConnexion", principal.getDateDerniereConnexion() != null
                        ? principal.getDateDerniereConnexion().toString()
                        : ""
        ));
    }*/

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(Map.of(
                "id", principal.getId(),
                "email", principal.getEmail(),
                "nomComplet", principal.getNomComplet(),
                "statut", principal.getStatut().name(),
                "nomDepartement", principal.getNomDepartement() != null ? principal.getNomDepartement() : "",
                "roles", principal.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority).collect(Collectors.toList()),
                "dateDerniereConnexion", principal.getDateDerniereConnexion() != null
                        ? principal.getDateDerniereConnexion().toString() : ""
        ));
    }
}
