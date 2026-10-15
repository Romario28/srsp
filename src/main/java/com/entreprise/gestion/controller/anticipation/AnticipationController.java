package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.dto.anticipation.AgentFicheDTO;
import com.entreprise.gestion.dto.anticipation.CalendrierAgentDTO;
import com.entreprise.gestion.dto.anticipation.CalendrierJourDTO;
import com.entreprise.gestion.dto.anticipation.EtatBaseAgentsDTO;
import com.entreprise.gestion.dto.anticipation.OptionsFiltresDTO;
import com.entreprise.gestion.service.anticipation.AnticipationService;
import com.entreprise.gestion.service.anticipation.CalendrierService;
import com.entreprise.gestion.service.anticipation.CritereDate;
import com.entreprise.gestion.service.anticipation.EcheanceAnticipeeDTO;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import com.entreprise.gestion.service.anticipation.FiltresAgentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/anticipation")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AnticipationController {

    private final AnticipationService anticipationService;
    private final CalendrierService calendrierService;
    private final FiltresAgentsService filtresAgentsService;

    @GetMapping("/base")
    public ResponseEntity<EtatBaseAgentsDTO> base() {
        return ResponseEntity.ok(new EtatBaseAgentsDTO(anticipationService.compterAgents()));
    }

    /** Valeurs proposées par la barre de filtres. */
    @GetMapping("/filtres")
    public ResponseEntity<OptionsFiltresDTO> filtres() {
        return ResponseEntity.ok(filtresAgentsService.options());
    }

    @GetMapping("/agents/{matricule}")
    public ResponseEntity<AgentFicheDTO> fiche(@PathVariable String matricule) {
        return ResponseEntity.ok(anticipationService.fiche(matricule));
    }

    // Filtres agents (statutAgent, corps, grade, categorie, fiv, soa, region, ministere) : tous facultatifs, cumulables
    @GetMapping("/calendrier")
    public ResponseEntity<List<CalendrierJourDTO>> calendrier(@RequestParam int annee,
                                                              @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(calendrierService.annee(annee, filtre));
    }

    @GetMapping("/calendrier/agents")
    public ResponseEntity<List<CalendrierAgentDTO>> calendrierAgents(
            @RequestParam LocalDate date,
            @RequestParam(required = false) CritereDate critereDate,
            @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(calendrierService.agents(date, critereDate, filtre));
    }

    @GetMapping("/calendrier/agents/mois")
    public ResponseEntity<List<CalendrierAgentDTO>> calendrierAgentsDuMois(
            @RequestParam int annee,
            @RequestParam int mois,   // 1 à 12
            @RequestParam(required = false) CritereDate critereDate,
            @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(calendrierService.agentsDuMois(annee, mois, critereDate, filtre));
    }

    @GetMapping("/retraite")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> retraite(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) CritereDate critereDate,
            @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(anticipationService.departsRetraite(prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre));
    }

    @GetMapping("/avancement")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> avancement(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) CritereDate critereDate,
            @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(anticipationService.avancementsDus(prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre));
    }

    @GetMapping("/titularisation")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> titularisation(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) CritereDate critereDate,
            @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(anticipationService.titularisationsDues(prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre));
    }

    @GetMapping("/fin-contrat")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> finContrat(
            @RequestParam(required = false) Integer prevenanceMois,
            @RequestParam(required = false) Integer retardMois,
            @RequestParam(required = false) LocalDate dateDebut,
            @RequestParam(required = false) LocalDate dateFin,
            @RequestParam(required = false) CritereDate critereDate,
            @ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(anticipationService.finsContrat(prevenanceMois, retardMois, dateDebut, dateFin, critereDate, filtre));
    }

    @GetMapping("/anomalies")
    public ResponseEntity<List<EcheanceAnticipeeDTO>> anomalies(@ModelAttribute FiltreAgents filtre) {
        return ResponseEntity.ok(anticipationService.anomalies(filtre));
    }
}
