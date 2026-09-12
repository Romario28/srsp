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
 * MIGRATION UserDetailsImpl :

 * Avant :  auditService.log(auth.getName(), action, details, ip)
 *          → AuditService faisait findByEmail() en interne pour attacher l'entité

 * Après :  auditService.logAvecEntite(principal.getUtilisateur(), action, details, ip)
 *          → L'entité arrive directement depuis le principal, zéro requête BDD

 * L'ancienne signature log(String email, ...) est conservée pour compatibilité
 * (DataInitializer, logout sans principal chargé, etc.)
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final LogAuditRepository   logAuditRepository;
    private final UtilisateurRepository utilisateurRepository;

    // ── Nouvelle méthode principale (migration UserDetailsImpl) ──────────────

    /**
     * Log avec l'entité Utilisateur directement disponible.
     * Utilisé quand on a déjà le principal (controllers, services post-auth).
     * Zéro requête BDD supplémentaire.
     */
    @Transactional
    public void logAvecEntite(Utilisateur utilisateur, String action,
                              String details, String ip) {
        LogAudit log = LogAudit.builder()
                .utilisateur(utilisateur)
                .action(action)
                .details(details)
                .adresseIp(ip)
                .build();
        logAuditRepository.save(log);
    }

    // ── Méthode historique conservée pour compatibilité ──────────────────────

    /**
     * Log avec l'email (fait un findByEmail en interne).
     * Conservé pour : logout, DataInitializer, cas sans principal chargé.
     */
    @Transactional
    public void log(String email, String action, String details, String ip) {
        LogAudit log = LogAudit.builder()
                .action(action)
                .details(details)
                .adresseIp(ip)
                .build();
        if (email != null) {
            utilisateurRepository.findByEmail(email)
                    .ifPresent(log::setUtilisateur);
        }
        logAuditRepository.save(log);
    }

    // ── Lecture ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<AuditDTO> findAll() {
        return logAuditRepository.findAllByOrderByDateActionDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private AuditDTO toDTO(LogAudit l) {
        return new AuditDTO(
                l.getId(),
                l.getUtilisateur() != null ? l.getUtilisateur().getEmail() : "système",
                l.getAction(),
                l.getDateAction(),
                l.getDetails(),
                l.getAdresseIp()
        );
    }
}
