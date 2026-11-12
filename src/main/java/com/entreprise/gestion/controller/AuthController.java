package com.entreprise.gestion.controller;

import com.entreprise.gestion.dto.DemandeCompteRequest;
import com.entreprise.gestion.dto.DepartementDTO;
import com.entreprise.gestion.dto.LoginRequest;
import com.entreprise.gestion.dto.LoginResponse;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.AuthService;
import com.entreprise.gestion.service.DemandeCompteService;
import com.entreprise.gestion.service.DepartementService;
import com.entreprise.gestion.service.acces.ControleAcces;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final ControleAcces controleAcces;
    private final DemandeCompteService demandeCompteService;   // [NOUVEAU]
    private final DepartementService departementService;       // [NOUVEAU]

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req,
                                   HttpServletRequest http) {
        try {
            LoginResponse resp = authService.login(req, http.getRemoteAddr());
            return ResponseEntity.ok(resp);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Email ou mot de passe incorrect"));
        } catch (BusinessException e) {   // [NOUVEAU] compte en attente de validation
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage(), "code", e.getCode()));
        } catch (DisabledException | LockedException e) {   // [MODIFIÉ] + LockedException
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Compte désactivé ou suspendu"));
        } catch (Exception e) {
            // [MODIFIÉ] ne plus renvoyer e.getMessage() : détail au journal, référence au client
            String reference = UUID.randomUUID().toString().substring(0, 8);
            log.error("Erreur serveur à la connexion [réf. {}]", reference, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Une erreur interne est survenue. Si elle persiste, contactez un administrateur en indiquant la référence "
                            + reference + ".",
                    "code", "ERREUR_INTERNE"));
        }
    }

    // [NOUVEAU] liste publique (formulaire de demande) : identifiant et libellé seulement
    @GetMapping("/departements")
    public ResponseEntity<List<DepartementDTO>> departements() {
        return ResponseEntity.ok(departementService.lister());
    }

    // [NOUVEAU] ↓ public : une demande ne donne aucun droit, un validateur doit l'approuver
    @PostMapping("/demande-compte")
    public ResponseEntity<Map<String, String>> demanderCompte(@Valid @RequestBody DemandeCompteRequest req,
                                                              HttpServletRequest http) {
        demandeCompteService.soumettre(req, http.getRemoteAddr());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message",
                "Votre demande est enregistrée. Elle sera examinée par un administrateur ou un chef : "
                        + "vous pourrez vous connecter dès qu'elle sera approuvée."));
    }
    // [NOUVEAU] ↑

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

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(Map.<String, Object>of(
                "id", principal.getId(),
                "email", principal.getEmail(),
                "nomComplet", principal.getNomComplet(),
                "statut", principal.getStatut().name(),
                "matriculeAgent", principal.getMatriculeAgent() != null ? principal.getMatriculeAgent() : "",
                "roles", principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()),
                "dateDerniereConnexion", principal.getDateDerniereConnexion() != null
                        ? principal.getDateDerniereConnexion().toString() : "",
                "peutTraiterSonDossier", controleAcces.exempteDuConflit(principal),
                "agirSurSonDossier", principal.isAgirSurSonDossier(),
                "perimetre", controleAcces.perimetreDe(principal)));
    }
}
