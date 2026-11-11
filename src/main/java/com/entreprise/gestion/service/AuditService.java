package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.AuditDTO;
import com.entreprise.gestion.entite.LogAudit;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.repository.LogAuditRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * L'action est toujours effectuée par l'UTILISATEUR. Si ce compte est lié à un agent, le journal retient son nom
 * complet et son matricule (libellé figé à l'écriture) au lieu de son seul e-mail.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final LogAuditRepository    logAuditRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Transactional
    public void logAvecEntite(Utilisateur utilisateur, String action, String details, String ip) {
        LogAudit log = LogAudit.builder()
                .utilisateur(utilisateur)
                .acteurLibelle(utilisateur != null ? utilisateur.libelleActeur() : null)   // [NOUVEAU]
                .action(action)
                .details(details)
                .adresseIp(ip)
                .build();
        logAuditRepository.save(log);
    }

    /** Conservée pour : logout, génération de dossiers (email null = système). */
    @Transactional
    public void log(String email, String action, String details, String ip) {
        LogAudit log = LogAudit.builder().action(action).details(details).adresseIp(ip).build();
        if (email != null) {
            utilisateurRepository.findByEmail(email).ifPresent(u -> {
                log.setUtilisateur(u);
                log.setActeurLibelle(u.libelleActeur());   // [NOUVEAU]
            });
        }
        logAuditRepository.save(log);
    }

    // [NOUVEAU] ↓ action sans compte connecté (demande de compte publique) : l'acteur est décrit en texte
    @Transactional
    public void logAnonyme(String acteurLibelle, String action, String details, String ip) {
        logAuditRepository.save(LogAudit.builder()
                .acteurLibelle(acteurLibelle).action(action).details(details).adresseIp(ip).build());
    }
    // [NOUVEAU] ↑

    @Transactional(readOnly = true)
    public List<AuditDTO> findAll() {
        return logAuditRepository.findAllByOrderByDateActionDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private AuditDTO toDTO(LogAudit l) {
        Utilisateur u = l.getUtilisateur();
        // Libellé figé ; pour les lignes antérieures à cette évolution, repli sur le libellé actuel du compte
        String acteur = l.getActeurLibelle() != null ? l.getActeurLibelle() : (u != null ? u.libelleActeur() : "Système");
        return new AuditDTO(l.getId(), u != null ? u.getEmail() : null, acteur,
                l.getAction(), l.getDateAction(), l.getDetails(), l.getAdresseIp());
    }
}
