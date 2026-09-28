package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.dto.anticipation.AlerteDTO;
import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.AlerteRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AlerteService {

    private final AlerteRepository alerteRepository;

    @Transactional(readOnly = true)
    public Page<AlerteDTO> lister(TypeAnticipation type, StatutAlerte statut, String matricule, Pageable page) {
        // Point d'extension futur : filtre de périmètre (VisibilityScope) si les alertes
        // sont rattachées à un périmètre organisationnel.
        return alerteRepository.rechercher(type, statut, matricule, page).map(AlerteDTO::from);
    }

    @Transactional
    public AlerteDTO consulter(Long id) {
        Alerte a = trouver(id);
        a.setDateDerniereConsultation(LocalDateTime.now());
        if (a.getStatut() == StatutAlerte.NOUVELLE) a.setStatut(StatutAlerte.VUE);
        return AlerteDTO.from(alerteRepository.save(a));
    }

    @Transactional
    public AlerteDTO acquitter(Long id, UserDetailsImpl actor) {
        Alerte a = trouver(id);
        if (a.getStatut() == StatutAlerte.ACQUITTEE) {
            throw new BusinessException("DEJA_ACQUITTEE", "Cette alerte est déjà acquittée.");
        }
        a.setStatut(StatutAlerte.ACQUITTEE);
        a.setDateAcquittement(LocalDateTime.now());
        a.setAcquitteePar(actor.getUtilisateur());
        return AlerteDTO.from(alerteRepository.save(a));
    }

    private Alerte trouver(Long id) {
        return alerteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ALERTE_INTROUVABLE", "Alerte introuvable : " + id));
    }
}
