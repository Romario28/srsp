// ImportController.java
package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.service.imports.ImportService;
import com.entreprise.gestion.service.imports.RapportImport;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@RestController
@RequestMapping("/api/imports")
@RequiredArgsConstructor
public class ImportController {

    private final ImportService importService;

    @PostMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RapportImport> importer(
            @PathVariable String type,
            @RequestParam("fichier") MultipartFile fichier) throws IOException {
        return ResponseEntity.ok(importService.importer(type, fichier.getInputStream()));
    }
}