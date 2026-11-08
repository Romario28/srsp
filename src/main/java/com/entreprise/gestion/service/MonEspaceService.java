package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.MonEspaceDTO;
import com.entreprise.gestion.dto.dossier.DossierDTO;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.DossierRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.anticipation.*;
import com.entreprise.gestion.service.dossier.CleDossier;
import com.entreprise.gestion.service.dossier.DossierResume;
import com.entreprise.gestion.service.dossier.SuiviDossiers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Espace personnel : un agent ne lit QUE l'agent de son propre matricule, jamais un identifiant reçu de l'extérieur. */
@Service
@RequiredArgsConstructor
public class MonEspaceService {

    private final AgentRepository agentRepository;
    private final DossierRepository dossierRepository;
    private final MoteurAnticipation moteur;
    private final ConfigurationDelaiService configurationDelaiService;
    private final GradeSuivantService gradeSuivantService;
    private final SuiviDossiers suiviDossiers;
    private final AnticipationService anticipationService;

    @Transactional(readOnly = true)
    public MonEspaceDTO consulter(UserDetailsImpl acteur) {
        String matricule = acteur.getMatriculeAgent();
        if (matricule == null || matricule.isBlank()) {
            throw new BusinessException("AUCUN_AGENT_LIE", "Votre compte n'est lié à aucun agent : contactez un administrateur.");
        }
        Agent agent = agentRepository.findById(matricule).orElseThrow(() -> new BusinessException("AGENT_INTROUVABLE",
                "Aucun agent n'a le matricule " + matricule + " dans la base pour l'instant."));

        LocalDate aujourd = LocalDate.now();
        boolean enActivite = agent.getSanction() == null
                || CodesSituationAdministrative.CODE_ACTIF.equals(agent.getSanction().getCode());

        List<EcheanceAnticipeeDTO> echeances = new ArrayList<>();
        List<String> manquantes = new ArrayList<>();
        if (enActivite) {
            Map<CleDossier, DossierResume> resumes = suiviDossiers.de(List.of(matricule));
            GradesParCorps grades = gradeSuivantService.indexer();
            for (Echeance e : moteur.calculerEcheances(agent)) {
                if (e.type() == TypeAnticipation.ANOMALIE) {
                    if (e.details() != null) manquantes.add(e.details());
                    continue;
                }
                DossierResume resume = resumes.get(new CleDossier(matricule, e.type(), e.dateEcheance()));
                echeances.add(EcheanceAnticipeeDTO.de(agent, e, configurationDelaiService.resoudreFenetre(e.type()),
                        e.type().changeDeGrade() ? grades : null, resume == null ? null : resume.etat(aujourd)));
            }
            echeances.sort(Comparator.comparing(EcheanceAnticipeeDTO::dateEcheance));
        }

        List<Dossier> dossiers = dossierRepository.findByAgentMatriculeOrderByDateEcheanceDesc(matricule);
        List<DossierDTO> dtos = dossiers.stream().map(d -> DossierDTO.de(d, aujourd, dossiers.size())).toList();

        return new MonEspaceDTO(anticipationService.ficheDe(agent), enActivite, acteur.isAgirSurSonDossier(),
                echeances, manquantes, dtos);
    }
}
