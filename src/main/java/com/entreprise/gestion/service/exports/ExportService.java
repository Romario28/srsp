package com.entreprise.gestion.service.exports;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.repository.anticipation.AlerteRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.entite.anticipation.Alerte;
import com.entreprise.gestion.entite.anticipation.StatutAlerte;
import com.entreprise.gestion.service.AuditService;
import com.entreprise.gestion.service.VisibiliteService;
import com.entreprise.gestion.service.anticipation.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;

@Service
public class ExportService {
    private static final List<String> BASE = List.of("POSTE_AGENT_NUMERO","AGENT_MATRICULE","AGENT_NOM","AGENT_PRENOMS","AGENT_DATE_NAIS","AGENT_CIN","AGENT_SEXE","STATUT","CORPS_CODE","GRADE_CODE","CATEGORIE","INDICE","HEE_CODE","HEE_CATEGORIE_CODE","SECTION_CODE","FIV_CODE","SANCTION_CODE","SOA","POSTE_AGENT_DATE_DEBUT_CONTRAT","POSTE_AGENT_DATE_FIN_CONTRAT","AVANCE_DATE","REG_CODE","MIN_CODE");
    private final AnticipationService anticipation;
    private final CalendrierService calendrier;
    private final VisibiliteService visibilite;
    private final AuditService audit;
    private final AlerteRepository alerteRepository;
    private final AgentRepository agentRepository;

    public ExportService(AnticipationService anticipation, CalendrierService calendrier, VisibiliteService visibilite, AuditService audit,
                         AlerteRepository alerteRepository, AgentRepository agentRepository) {
        this.anticipation=anticipation; this.calendrier=calendrier; this.visibilite=visibilite; this.audit=audit;
        this.alerteRepository=alerteRepository; this.agentRepository=agentRepository;
    }

    @Transactional(readOnly = true)
    public Fichier preparer(DemandeExport d, UserDetailsImpl acteur, String ip) {
        if (!visibilite.resoudreScope(acteur).isAccesTotal()) throw new AccessDeniedException("L'export est réservé aux comptes à accès total.");
        List<Object[]> lignes = new ArrayList<>();
        List<String> extra = new ArrayList<>();
        String periode = d.dateDebut()==null ? "tout" : d.dateDebut().toString() + (d.dateFin()==null?"":"_au_"+d.dateFin());
        FiltreAgents filtre = d.filtre()==null ? FiltreAgents.AUCUN : d.filtre();
        if (d.ecran()==Ecran.ALERTES) {
            extra.addAll(List.of("TYPE_ALERTE","DATE_PREPARATION","DATE_ECHEANCE","STATUT_ALERTE","DATE_DETECTION","DATE_ACQUITTEMENT","ACQUITTEE_PAR"));
            List<Alerte> alertes=alerteRepository.rechercher(d.type(),d.statutAlerte(),d.matricule(),filtre,Pageable.unpaged()).getContent();
            Map<String,Agent> agents=new HashMap<>();
            List<String> matricules=alertes.stream().map(Alerte::getMatriculeAgent).distinct().toList();
            for(int i=0;i<matricules.size();i+=500) agentRepository.findAvecReferences(matricules.subList(i,Math.min(i+500,matricules.size()))).forEach(a->agents.put(a.getMatricule(),a));
            for(Alerte a:alertes) {
                Object[] vals={a.getType().name(),a.getDatePreparation(),a.getDateEcheance(),a.getStatut().name(),a.getDateDetection(),a.getDateAcquittement(),a.getAcquitteePar()==null?null:a.getAcquitteePar().getEmail()};
                Agent agent=agents.get(a.getMatriculeAgent());
                if(agent!=null) lignes.add(ligneBase(agent,vals));
                else { Object[] base=new Object[BASE.size()+vals.length]; base[1]=a.getMatriculeAgent(); System.arraycopy(vals,0,base,BASE.size(),vals.length); lignes.add(base); }
            }
        } else if (d.ecran()==Ecran.ANOMALIES) {
            extra.addAll(List.of("TYPE_ANOMALIE","DETAILS"));
            anticipation.anomaliesAvecAgent(filtre).forEach(ae -> lignes.add(ligneBase(ae.agent(), ae.echeance().typeAnomalie()==null?null:ae.echeance().typeAnomalie().libelle(), ae.echeance().details())));
        } else if (d.ecran()==Ecran.CALENDRIER) {
            extra.addAll(List.of("TYPE","DATE_PREPARATION","DATE_ECHEANCE"));
            calendrier.pourExport(d.dateDebut(), d.dateFin(), d.critereDate(), d.typesCalendrier(), filtre).forEach(p -> lignes.add(ligneBase(p.agent(),p.categorie().name(),p.datePreparation(),p.dateEcheance())));
        } else {
            TypeAnticipation type = switch(d.ecran()) { case RETRAITE -> TypeAnticipation.DEPART_RETRAITE; case AVANCEMENT -> TypeAnticipation.AVANCEMENT; case TITULARISATION -> TypeAnticipation.TITULARISATION; case FIN_CONTRAT -> TypeAnticipation.FIN_CONTRAT; default -> throw new IllegalArgumentException("Écran non pris en charge"); };
            extra.addAll(List.of("DATE_PREPARATION","DATE_ECHEANCE","DETAILS"));
            anticipation.echeancesAvecAgent(type,d.prevenanceMois(),d.retardMois(),d.dateDebut(),d.dateFin(),d.critereDate(),filtre)
                    .forEach(ae -> lignes.add(ligneBase(ae.agent(),ae.ligne().datePreparation(),ae.ligne().dateEcheance(),ae.ligne().details())));
        }
        String nom = d.ecran().slug()+"_"+periode+"_"+LocalDate.now()+".xlsx";
        audit.logAvecEntite(acteur.getUtilisateur(),"EXPORT_EXCEL","Export « "+d.ecran().libelle()+" » : "+lignes.size()+" ligne(s), fichier "+nom,ip);
        return new Fichier(nom,lignes,extra);
    }

    private static Object[] ligneBase(Agent a,Object... extra) {
        var c=a.getCorps();
        Object[] base={a.getPosteNumero(),a.getMatricule(),a.getNom(),a.getPrenoms(),a.getDateNaissance(),a.getCin(),a.getSexe(),a.getStatut()==null?null:a.getStatut().name(),c==null?null:c.getCode(),a.getGrade()==null?null:a.getGrade().getCode(),c==null?null:c.getCategorie(),a.getIndiceActuel(),a.getHee()==null?null:a.getHee().getCode(),a.getHeeCategorieCode(),a.getSectionCode(),a.getLocalite()==null?null:a.getLocalite().getCode(),a.getSanction()==null?null:a.getSanction().getCode(),a.getSoa()==null?null:a.getSoa().getCode(),a.getDateDebutContrat(),a.getDateFinContrat(),a.getAvanceDate(),a.getRegCode(),a.getMinistere()==null?null:a.getMinistere().getCode()};
        Object[] all=Arrays.copyOf(base,base.length+extra.length); System.arraycopy(extra,0,all,base.length,extra.length); return all;
    }
    public record Fichier(String nom,List<Object[]> lignes,List<String> extra) {
        public void ecrireDans(OutputStream out) throws IOException {
            try(SXSSFWorkbook wb=new SXSSFWorkbook(100)) { wb.setCompressTempFiles(true); Sheet sh=wb.createSheet("Export"); Row h=sh.createRow(0); int i=0; for(String col:BASE) h.createCell(i++).setCellValue(col); for(String col:extra) h.createCell(i++).setCellValue(col);
                int n=1; for(Object[] vals:lignes) { Row r=sh.createRow(n++); for(int j=0;j<vals.length;j++) { Object v=vals[j]; if(v==null)continue; Cell c=r.createCell(j); if(v instanceof LocalDate d)c.setCellValue(d.toString()); else if(v instanceof Number x)c.setCellValue(x.doubleValue()); else c.setCellValue(v.toString()); }}
                wb.write(out); wb.dispose(); }
        }
    }
}
