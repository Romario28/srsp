package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.repository.anticipation.DossierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Component @RequiredArgsConstructor
public class SuiviDossiers {
    private static final int LOT=500;
    private final DossierRepository dossierRepository;
    @Transactional(readOnly=true)
    public Map<CleDossier,DossierResume> tous(){
        Map<CleDossier,DossierResume> result=new HashMap<>();dossierRepository.resumes().forEach(r->result.put(r.cle(),r));return result;
    }
    @Transactional(readOnly=true)
    public Map<CleDossier,DossierResume> de(Collection<String> matricules){
        List<String> distincts=matricules.stream().filter(Objects::nonNull).distinct().toList();
        Map<CleDossier,DossierResume> result=new HashMap<>();
        for(int i=0;i<distincts.size();i+=LOT)dossierRepository.resumesDe(distincts.subList(i,Math.min(i+LOT,distincts.size())))
                .forEach(r->result.put(r.cle(),r));
        return result;
    }
}
