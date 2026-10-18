package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.config.anticipation.DossierProperties;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.repository.anticipation.DossierRepository;
import com.entreprise.gestion.service.AuditService;
import com.entreprise.gestion.service.anticipation.Echeance;
import com.entreprise.gestion.service.anticipation.FenetreAnticipation;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Component @RequiredArgsConstructor @Slf4j
public class DossierGenerateur {
    private final DossierRepository dossierRepository;
    private final DossierProperties proprietes;
    private final EntityManager entityManager;
    private final AuditService auditService;
    public Passage ouvrir(LocalDate aujourdhui) {
        if(proprietes.getDateReprise()==null){log.warn("dossiers.date-reprise n'est pas défini : aucun dossier n'est créé.");return new Passage(aujourdhui,null);}
        return new Passage(aujourdhui,new HashSet<>(dossierRepository.cles()));
    }
    public final class Passage {
        private final LocalDate aujourdhui; private final Set<CleDossier> existants; private int crees;
        private Passage(LocalDate aujourdhui,Set<CleDossier> existants){this.aujourdhui=aujourdhui;this.existants=existants;}
        public void traiter(Agent agent,Echeance e,FenetreAnticipation fenetre) {
            if(existants==null||fenetre==null||e.type()==TypeAnticipation.ANOMALIE||e.dateEcheance()==null)return;
            if(proprietes.getTypesSuivis()==null||!proprietes.getTypesSuivis().contains(e.type()))return;
            LocalDate preparation=fenetre.datePreparation(e.dateEcheance());
            if(preparation.isAfter(aujourdhui)||preparation.isBefore(proprietes.getDateReprise()))return;
            if(!existants.add(new CleDossier(agent.getMatricule(),e.type(),e.dateEcheance())))return;
            dossierRepository.save(Dossier.builder().agent(entityManager.getReference(Agent.class,agent.getMatricule()))
                    .type(e.type()).datePreparation(preparation).dateEcheance(e.dateEcheance())
                    .statut(StatutDossier.EN_ATTENTE_DE_TRAITEMENT).build());
            crees++;
        }
        public int terminer() {
            if(crees>0)auditService.log(null,"DOSSIER_GENERATION",crees+" dossier(s) créé(s) (préparation atteinte, à partir du "+RegleDossier.fmt(proprietes.getDateReprise())+")",null);
            return crees;
        }
    }
}
