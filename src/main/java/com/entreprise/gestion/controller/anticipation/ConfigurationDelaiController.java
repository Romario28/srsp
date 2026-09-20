package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.dto.anticipation.ConfigurationDelaiDTO;
import com.entreprise.gestion.dto.anticipation.DefinirDelaiRequest;
import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.ConfigurationDelaiService;
import com.entreprise.gestion.service.anticipation.FenetreAnticipation;
import com.entreprise.gestion.service.anticipation.FenetresParDefaut;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * Pilotage en base des fenêtres d'anticipation (couple prévenance / retard par type).
 * La résolution « surcharge active sinon défaut » est entièrement déléguée à
 * {@link ConfigurationDelaiService} — la même que celle utilisée par le batch nocturne.
 */
@RestController
@RequestMapping("/api/anticipation/configuration")
@RequiredArgsConstructor
public class ConfigurationDelaiController {

    private final ConfigurationDelaiService configurationDelaiService;

    /** Liste les 4 types configurables avec leurs bornes effectives et leurs bornes par défaut. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<ConfigurationDelaiDTO>> lister() {
        List<ConfigurationDelaiDTO> resultat = Arrays.stream(TypeAnticipation.values())
                .filter(FenetresParDefaut::configurable)
                .map(this::vue)
                .toList();
        return ResponseEntity.ok(resultat);
    }

    /**
     * Crée ou met à jour la surcharge d'un type.
     * Exemple : {@code {"delaiPrevenanceJours": 120, "retardJours": 30}}.
     */
    @PutMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigurationDelaiDTO> definir(
            @PathVariable TypeAnticipation type,
            @Valid @RequestBody DefinirDelaiRequest req) {
        configurationDelaiService.definir(
                type, req.getDelaiPrevenanceJours(), req.getRetardJours(), req.getActif());
        return ResponseEntity.ok(vue(type));
    }

    /** Retire la surcharge — le type retombe sur son couple de valeurs par défaut. */
    @DeleteMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reinitialiser(@PathVariable TypeAnticipation type) {
        configurationDelaiService.reinitialiser(type);
        return ResponseEntity.noContent().build();
    }

    /** Reflète exactement ce que voit le moteur : effectif (résolu) + défaut + état en base. */
    private ConfigurationDelaiDTO vue(TypeAnticipation type) {
        FenetreAnticipation defaut = FenetresParDefaut.pour(type);
        FenetreAnticipation effective = configurationDelaiService.resoudreFenetre(type);
        ConfigurationDelai ligne = configurationDelaiService.configurationEnBase(type);

        Boolean actifEnBase = null;
        if (ligne != null) {
            actifEnBase = ligne.isActif();
        }

        return new ConfigurationDelaiDTO(
                type,
                effective.prevenanceJours(),
                effective.retardJours(),
                defaut.prevenanceJours(),
                defaut.retardJours(),
                ligne != null && ligne.isActif(),
                actifEnBase);
    }
}
