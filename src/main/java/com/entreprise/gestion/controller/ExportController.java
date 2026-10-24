package com.entreprise.gestion.controller;

import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.entite.anticipation.StatutAlerte;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.anticipation.CritereDate;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import com.entreprise.gestion.service.dossier.VueDossier;
import com.entreprise.gestion.service.exports.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/exports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ExportController {
    private static final String XLSX="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private final ExportService exportService;

    @GetMapping("/{ecran}")
    public void exporter(@PathVariable String ecran,
            @RequestParam(required=false) Integer prevenanceMois, @RequestParam(required=false) Integer retardMois,
            @RequestParam(required=false) LocalDate dateDebut, @RequestParam(required=false) LocalDate dateFin,
            @RequestParam(required=false) CritereDate critereDate,
            @RequestParam(required=false) Set<CategorieCalendrier> typesCalendrier,
            @RequestParam(required=false) TypeAnticipation type, @RequestParam(required=false) StatutAlerte statut,
            @RequestParam(required=false) String matricule,
            @RequestParam(required=false) VueDossier vue,
            @RequestParam(defaultValue="false") boolean inverse,
            @RequestParam(defaultValue="false") boolean parAgent,
            @RequestParam(defaultValue="false") boolean multiples,
            @ModelAttribute FiltreAgents filtre,
            @AuthenticationPrincipal UserDetailsImpl principal, HttpServletRequest requete,
            HttpServletResponse reponse) throws IOException {
        DemandeExport demande=new DemandeExport(Ecran.depuisSlug(ecran),filtre,prevenanceMois,retardMois,dateDebut,dateFin,critereDate,typesCalendrier,type,statut,matricule==null||matricule.isBlank()?null:matricule.trim(),vue,inverse,parAgent,multiples);
        ExportService.Fichier fichier=exportService.preparer(demande,principal,requete.getRemoteAddr());
        reponse.setContentType(XLSX);
        reponse.setHeader(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fichier.nom()).build().toString());
        reponse.setHeader(HttpHeaders.CACHE_CONTROL,"no-store");
        fichier.ecrireDans(reponse.getOutputStream());
    }
}
