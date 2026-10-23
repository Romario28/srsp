package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.dto.dossier.*;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import com.entreprise.gestion.service.dossier.DossierService;
import com.entreprise.gestion.service.dossier.VueDossier;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController @RequestMapping("/api/dossiers") @RequiredArgsConstructor @PreAuthorize("hasRole('ADMIN')")
public class DossierController {
    private final DossierService dossierService;
    @GetMapping("/par-echeance")
    public ResponseEntity<DossierDTO> parEcheance(@RequestParam String matricule,@RequestParam TypeAnticipation type,@RequestParam LocalDate dateEcheance) {
        return ResponseEntity.ok(dossierService.parEcheance(matricule.trim(),type,dateEcheance));
    }
    @GetMapping
    public ResponseEntity<Page<DossierDTO>> lister(@RequestParam(defaultValue="A_TRAITER") VueDossier vue,
            @RequestParam(defaultValue="false") boolean inverse,
            @RequestParam(required=false) TypeAnticipation type,@RequestParam(required=false) String matricule,
            @ModelAttribute FiltreAgents filtre,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return ResponseEntity.ok(dossierService.lister(vue,inverse,type,matricule==null||matricule.isBlank()?null:matricule.trim(),filtre,page,size));
    }
    @GetMapping("/synthese") public ResponseEntity<SyntheseDossiersDTO> synthese(
            @RequestParam(required=false) TypeAnticipation type,@RequestParam(required=false) String matricule,
            @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(dossierService.synthese(type,matricule==null||matricule.isBlank()?null:matricule.trim(),filtre));
    }
    @PostMapping("/{id}/demarrage")
    public ResponseEntity<DossierDTO> demarrer(@PathVariable Long id, @AuthenticationPrincipal UserDetailsImpl principal) {
        return ResponseEntity.ok(dossierService.demarrer(id, principal));
    }
    @PostMapping("/{id}/sans-suite") public ResponseEntity<DossierDTO> sansSuite(@PathVariable Long id,@Valid @RequestBody SansSuiteRequest req,@AuthenticationPrincipal UserDetailsImpl principal){
        return ResponseEntity.ok(dossierService.classerSansSuite(id,req.motif(),principal));
    }
    @PostMapping("/{id}/reouverture") public ResponseEntity<DossierDTO> reouverture(@PathVariable Long id,@AuthenticationPrincipal UserDetailsImpl principal){
        return ResponseEntity.ok(dossierService.reouvrir(id,principal));
    }
}
