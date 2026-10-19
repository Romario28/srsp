package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.config.anticipation.DossierProperties;
import com.entreprise.gestion.dto.dossier.*;
import com.entreprise.gestion.dto.dossier.ResultatLot.Refus;
import com.entreprise.gestion.entite.anticipation.*;
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
import java.util.*;
import java.util.stream.Collectors;
import static com.entreprise.gestion.service.dossier.RegleDossier.fmt;

@Service @RequiredArgsConstructor
public class DossierService {
    private static final int LOT_MAX=500, AUDIT_MAX=500;
    private final DossierRepository dossierRepository;
    private final DossierProperties proprietes;
    private final AuditService auditService;

    @Transactional(readOnly=true)
    public Page<DossierDTO> lister(VueDossier vue,boolean inverse,TypeAnticipation type,String matricule,FiltreAgents filtre,int page,int size) {
        LocalDate aujourd=LocalDate.now();
        Pageable pageable=PageRequest.of(Math.max(page,0),Math.min(Math.max(size,1),200),vue.tri(inverse));
        return dossierRepository.rechercher(vue.statuts(),type,matricule,vue.bornes(aujourd),
                filtre==null?FiltreAgents.AUCUN:filtre,pageable)
                .map(d->vers(d,aujourd));
    }
    @Transactional(readOnly=true)
    public SyntheseDossiersDTO synthese() {
        LocalDate aujourd=LocalDate.now();
        return new SyntheseDossiersDTO(dossierRepository.compterATraiter(aujourd),
                dossierRepository.compterEnRetard(aujourd),dossierRepository.countByStatut(StatutDossier.EN_COURS),
                dossierRepository.countByStatut(StatutDossier.TRAITE),dossierRepository.countByStatut(StatutDossier.SANS_SUITE),
                dossierRepository.compterARelancer(aujourd.minusDays(proprietes.getRelanceApresJours())),
                proprietes.getDateReprise(),proprietes.getRelanceApresJours());
    }
    @Transactional
    public DossierDTO mettreAJourEtapes(Long id,Long version,Map<EtapeDossier,LocalDate> demandees,UserDetailsImpl acteur) {
        Dossier d=trouver(id);
        if(!Objects.equals(d.getVersion(),version))throw new BusinessException("DOSSIER_MODIFIE","Ce dossier a été modifié entre-temps. Actualisez la liste puis recommencez.");
        exigerNonSansSuite(d); StatutDossier avant=d.getStatut(); List<String> changements=new ArrayList<>();
        for(EtapeDossier e:EtapeDossier.values()) {
            if(!demandees.containsKey(e))continue;
            LocalDate ancien=e.de(d),nouveau=demandees.get(e); if(Objects.equals(ancien,nouveau))continue;
            e.definir(d,nouveau); changements.add(e.libelle()+" : "+fmt(ancien)+" → "+fmt(nouveau));
        }
        if(!changements.isEmpty()) {
            RegleDossier.verifier(d,LocalDate.now()); d.setStatut(RegleDossier.statutSelonEtapes(d)); dossierRepository.saveAndFlush(d);
            String suffixe=statutModifie(avant,d.getStatut());
            changements.forEach(c->tracer(acteur,"DOSSIER_ETAPE",decrire(d)+" — "+c+suffixe));
        }
        return vers(d,LocalDate.now());
    }
    @Transactional
    public ResultatLot appliquerLot(List<Long> ids,EtapeDossier etape,LocalDate date,boolean ecraser,UserDetailsImpl acteur) {
        if(ids.size()>LOT_MAX)throw new BusinessException("LOT_TROP_GRAND","Un lot ne peut pas dépasser "+LOT_MAX+" dossiers.");
        LocalDate aujourd=LocalDate.now(); if(date.isAfter(aujourd))throw new BusinessException("DATE_FUTURE","La date "+fmt(date)+" est dans le futur.");
        Map<Long,Dossier> trouves=dossierRepository.avecAgents(ids).stream().collect(Collectors.toMap(Dossier::getId, d->d));
        List<Refus> refuses=new ArrayList<>(); int modifies=0,inchanges=0;
        for(Long id:new LinkedHashSet<>(ids)) {
            Dossier d=trouves.get(id);
            if(d==null){refuses.add(new Refus(id,"#"+id,"dossier introuvable"));continue;}
            String libelle=d.getAgent().getMatricule()+" — "+d.getAgent().getPrenoms()+" "+d.getAgent().getNom();
            if(d.getStatut()==StatutDossier.SANS_SUITE){refuses.add(new Refus(id,libelle,"classé sans suite : à rouvrir d'abord"));continue;}
            LocalDate ancien=etape.de(d); if(date.equals(ancien)){inchanges++;continue;}
            if(ancien!=null&&!ecraser){refuses.add(new Refus(id,libelle,"« "+etape.libelle()+" » déjà renseignée au "+fmt(ancien)+" (cochez « Remplacer les dates déjà saisies » pour la modifier)"));continue;}
            StatutDossier avant=d.getStatut(); etape.definir(d,date);
            try{RegleDossier.verifier(d,aujourd);}catch(BusinessException ex){etape.definir(d,ancien);refuses.add(new Refus(id,libelle,ex.getMessage()));continue;}
            d.setStatut(RegleDossier.statutSelonEtapes(d)); modifies++;
            tracer(acteur,"DOSSIER_ETAPE",decrire(d)+" — "+etape.libelle()+" : "+fmt(ancien)+" → "+fmt(date)+" (lot)"+statutModifie(avant,d.getStatut()));
        }
        return new ResultatLot(modifies,inchanges,refuses);
    }
    @Transactional
    public DossierDTO classerSansSuite(Long id,String motif,UserDetailsImpl acteur) {
        Dossier d=trouver(id); String nettoye=motif==null?"":motif.trim();
        if(nettoye.isEmpty())throw new BusinessException("MOTIF_REQUIS","Le motif est obligatoire pour classer un dossier sans suite.");
        if(d.getStatut()==StatutDossier.TRAITE||d.getStatut()==StatutDossier.SANS_SUITE)throw new BusinessException("DOSSIER_CLOS","Ce dossier est déjà clos.");
        StatutDossier avant=d.getStatut();d.setStatut(StatutDossier.SANS_SUITE);d.setMotifSansSuite(nettoye);dossierRepository.saveAndFlush(d);
        tracer(acteur,"DOSSIER_SANS_SUITE",decrire(d)+" — motif : "+nettoye+statutModifie(avant,d.getStatut())); return vers(d,LocalDate.now());
    }
    @Transactional
    public DossierDTO reouvrir(Long id,UserDetailsImpl acteur) {
        Dossier d=trouver(id);if(d.getStatut()!=StatutDossier.SANS_SUITE)throw new BusinessException("DOSSIER_NON_CLASSE","Ce dossier n'est pas classé sans suite.");
        String motif=d.getMotifSansSuite();d.setMotifSansSuite(null);d.setStatut(RegleDossier.statutSelonEtapes(d));dossierRepository.saveAndFlush(d);
        tracer(acteur,"DOSSIER_REOUVERT",decrire(d)+" — rouvert (ancien motif : "+motif+") → "+d.getStatut());return vers(d,LocalDate.now());
    }
    private Dossier trouver(Long id){return dossierRepository.findById(id).orElseThrow(()->new BusinessException("DOSSIER_INTROUVABLE","Dossier introuvable : "+id));}
    private static void exigerNonSansSuite(Dossier d){if(d.getStatut()==StatutDossier.SANS_SUITE)throw new BusinessException("DOSSIER_SANS_SUITE","Ce dossier est classé sans suite : rouvrez-le avant de saisir une étape.");}
    private DossierDTO vers(Dossier d,LocalDate aujourd){return DossierDTO.de(d,aujourd,proprietes.getRelanceApresJours());}
    private static String decrire(Dossier d){return "Dossier #"+d.getId()+" "+d.getAgent().getMatricule()+" "+d.getType();}
    private static String statutModifie(StatutDossier a,StatutDossier b){return a==b?"":" (statut : "+a+" → "+b+")";}
    private void tracer(UserDetailsImpl acteur,String action,String details){String texte=details.length()<=AUDIT_MAX?details:details.substring(0,AUDIT_MAX-1)+"…";auditService.logAvecEntite(acteur.getUtilisateur(),action,texte,null);}
}
