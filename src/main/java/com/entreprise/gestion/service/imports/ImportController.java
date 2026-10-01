// ImportController.java
package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @RequestParam("fichier") MultipartFile fichier,
            @AuthenticationPrincipal UserDetailsImpl principal) throws IOException {
        return ResponseEntity.ok(importService.importer(type, fichier.getInputStream(), fichier.getOriginalFilename(), principal));
    }
}
