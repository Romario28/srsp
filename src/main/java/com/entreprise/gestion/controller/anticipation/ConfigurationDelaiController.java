package com.entreprise.gestion.controller.anticipation;

import com.entreprise.gestion.dto.anticipation.ConfigurationDelaiDTO;
import com.entreprise.gestion.dto.anticipation.DefinirDelaiRequest;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.service.anticipation.ConfigurationDelaiService;
import com.entreprise.gestion.service.anticipation.DelaisParDefaut;
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

    /** Liste les 4 types configurables avec leur délai effectif et leur défaut. */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYE')")
    public ResponseEntity<List<ConfigurationDelaiDTO>> lister() {
        Map<TypeAnticipation, Integer> effectifs = configurationDelaiService.resoudreTous();

        List<ConfigurationDelaiDTO> resultat = Arrays.stream(TypeAnticipation.values())
                .filter(t -> t != TypeAnticipation.ANOMALIE)
                .map(t -> {
                    int defaut = DelaisParDefaut.pour(t);
                    int effectif = effectifs.get(t);
                    return new ConfigurationDelaiDTO(t, effectif, defaut, effectif != defaut);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(resultat);
    }

    @PutMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ConfigurationDelaiDTO> definir(
            @PathVariable TypeAnticipation type,
            @Valid @RequestBody DefinirDelaiRequest req) {
        configurationDelaiService.definir(type, req.getDelaiPrevenanceJours());
        return ResponseEntity.ok(new ConfigurationDelaiDTO(
                type, req.getDelaiPrevenanceJours(), DelaisParDefaut.pour(type), true));
    }

    /** Retire la surcharge — le type retombe sur sa valeur par défaut. */
    @DeleteMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reinitialiser(@PathVariable TypeAnticipation type) {
        configurationDelaiService.reinitialiser(type);
        return ResponseEntity.noContent().build();
    }
}