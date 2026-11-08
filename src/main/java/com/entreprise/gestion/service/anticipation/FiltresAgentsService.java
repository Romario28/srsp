package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.dto.anticipation.OptionFiltre;
import com.entreprise.gestion.dto.anticipation.OptionsFiltresDTO;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.service.acces.Perimetre;
import com.entreprise.gestion.service.acces.PerimetreCourant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FiltresAgentsService {

    private final AgentRepository agentRepository;
    private final PerimetreCourant perimetreCourant;

    @Transactional(readOnly = true)
    public OptionsFiltresDTO options() {
        Perimetre p = perimetreCourant.get();
        return new OptionsFiltresDTO(
                agentRepository.optionsCorps(),
                agentRepository.optionsGrades(),
                sansLibelle(agentRepository.categories()),
                limiter(agentRepository.optionsFiv(), p, p.restreintFivs(), p.fivs()),
                limiter(agentRepository.optionsSoa(), p, p.restreintSoas(), p.soas()),
                sansLibelle(agentRepository.regions()),
                limiter(agentRepository.optionsMinisteres(), p, p.restreintMinisteres(), p.ministeres()));
    }

    private static List<OptionFiltre> limiter(List<OptionFiltre> options, Perimetre p, boolean restreint, Set<String> permis) {
        if (p.estVide()) return List.of();
        return restreint ? options.stream().filter(o -> permis.contains(o.code())).toList() : options;
    }

    /** Catégories et régions n'ont pas de référentiel : le code seul. */
    private static List<OptionFiltre> sansLibelle(List<String> codes) {
        return codes.stream().map(code -> new OptionFiltre(code, null)).toList();
    }
}
