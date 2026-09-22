package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.dto.anticipation.ConfigurationDelaiDTO;
import com.entreprise.gestion.dto.anticipation.DefinirDelaiRequest;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.ConfigurationDelaiService;
import com.entreprise.gestion.service.anticipation.DelaisParDefaut;
import com.entreprise.gestion.service.anticipation.FenetreAnticipation; // AJOUTÉ
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/anticipation/configuration")
@RequiredArgsConstructor
public class ConfigurationDelaiController {

    private final ConfigurationDelaiService configurationDelaiService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<ConfigurationDelaiDTO>> lister() {
        Map<TypeAnticipation, FenetreAnticipation> effectives = configurationDelaiService.resoudreToutes(); // MODIFIÉ

        List<ConfigurationDelaiDTO> resultat = Arrays.stream(TypeAnticipation.values())
                .filter(t -> t != TypeAnticipation.ANOMALIE)
                .map(t -> {
                    FenetreAnticipation defaut = DelaisParDefaut.pour(t);     // MODIFIÉ
                    FenetreAnticipation effectif = effectives.get(t);        // MODIFIÉ
                    // MODIFIÉ — DTO alimenté avec les 2 bornes au lieu d'une seule
                    return new ConfigurationDelaiDTO(
                            t, effectif.prevenanceJours(), effectif.retardJours(),
                            defaut.prevenanceJours(), defaut.retardJours(),
                            !effectif.equals(defaut));
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultat);
    }

    @PutMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigurationDelaiDTO> definir(
            @PathVariable TypeAnticipation type,
            @Valid @RequestBody DefinirDelaiRequest req) {
        configurationDelaiService.definir(type, req.getPrevenanceJours(), req.getRetardJours()); // MODIFIÉ
        FenetreAnticipation defaut = DelaisParDefaut.pour(type);   // MODIFIÉ
        return ResponseEntity.ok(new ConfigurationDelaiDTO(
                type, req.getPrevenanceJours(), req.getRetardJours(),
                defaut.prevenanceJours(), defaut.retardJours(), true));
    }

    // reinitialiser() — inchangée
    @DeleteMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reinitialiser(@PathVariable TypeAnticipation type) {
        configurationDelaiService.reinitialiser(type);
        return ResponseEntity.noContent().build();
    }
}
