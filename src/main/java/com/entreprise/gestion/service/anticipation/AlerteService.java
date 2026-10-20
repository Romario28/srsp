package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.dto.anticipation.AlerteDTO;
import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.AlerteRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.dossier.CleDossier;
import com.entreprise.gestion.service.dossier.DossierResume;
import com.entreprise.gestion.service.dossier.EtatDossier;
import com.entreprise.gestion.service.dossier.SuiviDossiers;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final SuiviDossiers suiviDossiers;

    @Transactional(readOnly = true)
    public Page<AlerteDTO> lister(TypeAnticipation type, StatutAlerte statut, String matricule,
                                  FiltreAgents filtre, Pageable page) {
        // Point d'extension futur : filtre de périmètre (VisibilityScope) si les alertes
        // sont rattachées à un périmètre organisationnel.
        Page<Alerte> alertes=alerteRepository.rechercher(type,statut,matricule,filtre,page);
        Map<CleDossier,DossierResume> dossiers=suiviDossiers.de(alertes.getContent().stream().map(Alerte::getMatriculeAgent).toList());
        LocalDate aujourd=LocalDate.now();
        return alertes.map(a->AlerteDTO.from(a,etatDossier(a,dossiers,aujourd)));
    }

    @Transactional
    public AlerteDTO consulter(Long id) {
        Alerte a = trouver(id);
        a.setDateDerniereConsultation(LocalDateTime.now());
        if (a.getStatut() == StatutAlerte.NOUVELLE) a.setStatut(StatutAlerte.VUE);
        return vers(alerteRepository.save(a));
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
        return vers(alerteRepository.save(a));
    }

    private AlerteDTO vers(Alerte a) {
        Map<CleDossier,DossierResume> dossiers=suiviDossiers.de(List.of(a.getMatriculeAgent()));
        return AlerteDTO.from(a,etatDossier(a,dossiers,LocalDate.now()));
    }

    private static EtatDossier etatDossier(Alerte a,Map<CleDossier,DossierResume> dossiers,LocalDate aujourd) {
        if(a.getDateEcheance()==null)return null;
        DossierResume dossier=dossiers.get(new CleDossier(a.getMatriculeAgent(),a.getType(),a.getDateEcheance()));
        return dossier==null?null:dossier.etat(aujourd);
    }

    private Alerte trouver(Long id) {
        return alerteRepository.findById(id)
                .orElseThrow(() -> new BusinessException("ALERTE_INTROUVABLE", "Alerte introuvable : " + id));
    }
}
