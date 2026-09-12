// service/anticipation/AlerteService.java
package com.entreprise.gestion.service.anticipation;

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
    public Page<Alerte> lister(TypeAnticipation type, StatutAlerte statut, String matricule, Pageable page) {
        return alerteRepository.rechercher(type, statut, matricule, page);
    }

    @Transactional
    public Alerte consulter(Long id) {
        Alerte a = trouver(id);
        a.setDateDerniereConsultation(LocalDateTime.now());
        if (a.getStatut() == StatutAlerte.NOUVELLE) a.setStatut(StatutAlerte.VUE);
        return alerteRepository.save(a);
    }

    @Transactional
    public Alerte acquitter(Long id, UserDetailsImpl actor) {
        Alerte a = trouver(id);
        if (a.getStatut() == StatutAlerte.ACQUITTEE) {
            throw new BusinessException("DEJA_ACQUITTEE", "Cette alerte est déjà acquittée.");
        }
        a.setStatut(StatutAlerte.ACQUITTEE);
        a.setDateAcquittement(LocalDateTime.now());
        a.setAcquitteePar(actor.getUtilisateur());
        return alerteRepository.save(a);
    }

    private Alerte trouver(Long id) {
        return alerteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ALERTE_INTROUVABLE", "Alerte introuvable : " + id));
    }


}