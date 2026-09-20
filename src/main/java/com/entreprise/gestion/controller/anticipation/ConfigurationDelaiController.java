package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.dto.anticipation.ConfigurationDelaiDTO;
import com.entreprise.gestion.dto.anticipation.DefinirDelaiRequest;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.ConfigurationDelaiService;
import com.entreprise.gestion.service.anticipation.FenetreAnticipation;
import com.entreprise.gestion.service.anticipation.FenetresParDefaut;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Paramétrage des fenêtres d'anticipation (prévenance / retard) par type.
 * Aucune de ces valeurs n'est figée dans le code : une surcharge enregistrée ici
 * (ou directement en base) est prise en compte par le batch nocturne comme par
 * les endpoints de consultation, sans redéploiement.
 */
@RestController
@RequestMapping("/api/anticipation/configuration")
@RequiredArgsConstructor
public class ConfigurationDelaiController {

    private final ConfigurationDelaiService configurationDelaiService;

    /** Les 4 types configurables, avec leurs bornes effectives et leurs valeurs par défaut. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<ConfigurationDelaiDTO>> lister() {
        Map<TypeAnticipation, FenetreAnticipation> effectives = configurationDelaiService.resoudreToutes();

        List<ConfigurationDelaiDTO> resultat = FenetresParDefaut.typesConfigurables().stream()
                .map(type -> versDto(type, effectives.get(type)))
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultat);
    }

    /** Crée ou met à jour la fenêtre d'un type — une borne omise reste inchangée. */
    @PutMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigurationDelaiDTO> definir(
            @PathVariable TypeAnticipation type,
            @Valid @RequestBody DefinirDelaiRequest req) {
        configurationDelaiService.definir(
                type, req.getDelaiPrevenanceJours(), req.getRetardJours(), req.getActif());
        return ResponseEntity.ok(versDto(type, configurationDelaiService.resoudrePlage(type)));
    }

    /** Retire la surcharge — le type retombe sur ses valeurs par défaut. */
    @DeleteMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reinitialiser(@PathVariable TypeAnticipation type) {
        configurationDelaiService.reinitialiser(type);
        return ResponseEntity.noContent().build();
    }

    private ConfigurationDelaiDTO versDto(TypeAnticipation type, FenetreAnticipation effective) {
        FenetreAnticipation defaut = FenetresParDefaut.pour(type);
        return new ConfigurationDelaiDTO(
                type,
                effective.prevenanceJours(), defaut.prevenanceJours(),
                effective.retardJours(), defaut.retardJours(),
                configurationDelaiService.estPersonnalise(type));
    }
}
