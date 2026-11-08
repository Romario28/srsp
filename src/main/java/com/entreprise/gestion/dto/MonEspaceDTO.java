package com.entreprise.gestion.dto;

import com.entreprise.gestion.dto.anticipation.AgentFicheDTO;
import com.entreprise.gestion.dto.dossier.DossierDTO;
import com.entreprise.gestion.service.anticipation.EcheanceAnticipeeDTO;

import java.util.List;

/**
 * Ce que voit un agent : sa fiche, ses échéances (vides s'il n'est pas « en activité »), les données qui manquent
 * pour calculer le reste, ses dossiers, et son droit d'agir sur son propre dossier.
 */
public record MonEspaceDTO(AgentFicheDTO fiche, boolean enActivite, boolean agirSurSonDossier,
                           List<EcheanceAnticipeeDTO> echeances, List<String> donneesManquantes,
                           List<DossierDTO> dossiers) {}
