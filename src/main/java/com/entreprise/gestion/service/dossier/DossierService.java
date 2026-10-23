package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.config.anticipation.DossierProperties;
import com.entreprise.gestion.dto.dossier.DossierDTO;
import com.entreprise.gestion.dto.dossier.SyntheseDossiersDTO;
import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.DossierRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.AuditService;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import static com.entreprise.gestion.service.dossier.RegleDossier.fmt;

@Service
@RequiredArgsConstructor
public class DossierService {
    private static final int AUDIT_MAX = 500;
    private final DossierRepository dossierRepository;
    private final DossierProperties proprietes;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public Page<DossierDTO> lister(VueDossier vue, boolean inverse, TypeAnticipation type, String matricule,
                                   FiltreAgents filtre, int page, int size) {
        LocalDate aujourd = LocalDate.now();
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), vue.tri(inverse));
        return dossierRepository.rechercher(vue.statuts(), type, matricule, vue.bornes(aujourd),
                filtre == null ? FiltreAgents.AUCUN : filtre, pageable).map(d -> DossierDTO.de(d, aujourd));
    }

    @Transactional(readOnly = true)
    public SyntheseDossiersDTO synthese(TypeAnticipation type, String matricule, FiltreAgents filtre) {
        LocalDate aujourd = LocalDate.now();
        FiltreAgents f = filtre == null ? FiltreAgents.AUCUN : filtre;
        return new SyntheseDossiersDTO(
                compter(VueDossier.A_TRAITER, type, matricule, f, aujourd),
                compter(VueDossier.EN_RETARD, type, matricule, f, aujourd),
                compter(VueDossier.EN_COURS, type, matricule, f, aujourd),
                compter(VueDossier.SANS_SUITE, type, matricule, f, aujourd),
                proprietes.getDateReprise());
    }

    private long compter(VueDossier vue, TypeAnticipation type, String matricule, FiltreAgents filtre, LocalDate aujourd) {
        return dossierRepository.compter(vue.statuts(), type, matricule, vue.bornes(aujourd), filtre);
    }

    @Transactional(readOnly = true)
    public List<Dossier> pourExport(VueDossier vue, boolean inverse, TypeAnticipation type, String matricule, FiltreAgents filtre) {
        return dossierRepository.rechercherTous(vue.statuts(), type, matricule, vue.bornes(LocalDate.now()),
                filtre == null ? FiltreAgents.AUCUN : filtre, vue.tri(inverse));
    }

    @Transactional(readOnly = true)
    public DossierDTO parEcheance(String matricule, TypeAnticipation type, LocalDate dateEcheance) {
        Dossier d = dossierRepository.findByAgentMatriculeAndTypeAndDateEcheance(matricule, type, dateEcheance)
                .orElseThrow(() -> new BusinessException("DOSSIER_INTROUVABLE",
                        "Aucun dossier pour cette échéance (" + matricule + ", " + type + ", " + fmt(dateEcheance) + ")."));
        return DossierDTO.de(d, LocalDate.now());
    }

    /** À traiter ou en retard → en cours. La date du jour est posée par le serveur. */
    @Transactional
    public DossierDTO demarrer(Long id, UserDetailsImpl acteur) {
        Dossier d = trouver(id);
        if (d.getStatut() == StatutDossier.EN_COURS) {
            throw new BusinessException("DEJA_DEMARRE", "Ce dossier est déjà en cours : actualisez la liste.");
        }
        if (d.getStatut() == StatutDossier.SANS_SUITE) {
            throw new BusinessException("DOSSIER_SANS_SUITE", "Ce dossier est classé sans suite : rouvrez-le avant de le démarrer.");
        }
        LocalDate aujourd = LocalDate.now();
        boolean enRetard = RegleDossier.enRetard(d, aujourd);
        d.setStatut(StatutDossier.EN_COURS);
        d.setDateDemarrage(aujourd);
        dossierRepository.saveAndFlush(d);
        tracer(acteur, "DOSSIER_DEMARRE", decrire(d) + " — démarré le " + fmt(aujourd)
                + (enRetard ? " (démarré en retard, échéance le " + fmt(d.getDateEcheance()) + ")" : ""));
        return DossierDTO.de(d, aujourd);
    }

    @Transactional
    public DossierDTO classerSansSuite(Long id, String motif, UserDetailsImpl acteur) {
        Dossier d = trouver(id);
        String nettoye = motif == null ? "" : motif.trim();
        if (nettoye.isEmpty()) throw new BusinessException("MOTIF_REQUIS", "Le motif est obligatoire pour classer un dossier sans suite.");
        if (d.getStatut() == StatutDossier.SANS_SUITE) throw new BusinessException("DOSSIER_CLOS", "Ce dossier est déjà classé sans suite.");
        StatutDossier avant = d.getStatut();
        d.setStatut(StatutDossier.SANS_SUITE);
        d.setMotifSansSuite(nettoye);
        dossierRepository.saveAndFlush(d);
        tracer(acteur, "DOSSIER_SANS_SUITE", decrire(d) + " — motif : " + nettoye + " (statut : " + avant + " → " + d.getStatut() + ")");
        return DossierDTO.de(d, LocalDate.now());
    }

    @Transactional
    public DossierDTO reouvrir(Long id, UserDetailsImpl acteur) {
        Dossier d = trouver(id);
        if (d.getStatut() != StatutDossier.SANS_SUITE) throw new BusinessException("DOSSIER_NON_CLASSE", "Ce dossier n'est pas classé sans suite.");
        String motif = d.getMotifSansSuite();
        d.setMotifSansSuite(null);
        d.setStatut(RegleDossier.statutApresReouverture(d));
        dossierRepository.saveAndFlush(d);
        tracer(acteur, "DOSSIER_REOUVERT", decrire(d) + " — rouvert (ancien motif : " + motif + ") → " + d.getStatut());
        return DossierDTO.de(d, LocalDate.now());
    }

    private Dossier trouver(Long id) {
        return dossierRepository.findById(id).orElseThrow(() -> new BusinessException("DOSSIER_INTROUVABLE", "Dossier introuvable : " + id));
    }
    private static String decrire(Dossier d) { return "Dossier #" + d.getId() + " " + d.getAgent().getMatricule() + " " + d.getType(); }
    private void tracer(UserDetailsImpl acteur, String action, String details) {
        String texte = details.length() <= AUDIT_MAX ? details : details.substring(0, AUDIT_MAX - 1) + "…";
        auditService.logAvecEntite(acteur.getUtilisateur(), action, texte, null);
    }
}