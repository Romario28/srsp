package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.dto.anticipation.EtatBaseAgentsDTO;
import com.entreprise.gestion.service.anticipation.EcheanceAnticipeeDTO;
import com.entreprise.gestion.service.anticipation.AnticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate; // AJOUTÉ
import java.util.List;

@RestController
@RequestMapping("/api/anticipation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnticipationController {

    private final AnticipationService anticipationService;

    @GetMapping("/base")
    public ResponseEntity<EtatBaseAgentsDTO> base() {
        return ResponseEntity.ok(new EtatBaseAgentsDTO(anticipationService.compterAgents()));
    }

    // MODIFIÉ — ajout dateDebut/dateFin (format ISO attendu : yyyy-MM-dd, ex. ?dateDebut=2026-01-01)
    @GetMapping("/retraite")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> retraite(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.departsRetraite(prevenanceMois, retardMois, dateDebut, dateFin, statut));
    }

    @GetMapping("/avancement")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> avancement(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.avancementsDus(prevenanceMois, retardMois, dateDebut, dateFin, statut));
    }

    @GetMapping("/titularisation")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> titularisation(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.titularisationsDues(prevenanceMois, retardMois, dateDebut, dateFin, statut));
    }

    @GetMapping("/fin-contrat")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> finContrat(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.finsContrat(prevenanceMois, retardMois, dateDebut, dateFin, statut));
    }
    @GetMapping("/anomalies")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> anomalies(
            @RequestParam(required = false) StatutAgent statut) {
        return ResponseEntity.ok(anticipationService.anomalies(statut));
    }
}
