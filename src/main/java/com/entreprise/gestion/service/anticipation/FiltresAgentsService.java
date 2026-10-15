package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.dto.anticipation.OptionFiltre;
import com.entreprise.gestion.dto.anticipation.OptionsFiltresDTO;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FiltresAgentsService {

    private final AgentRepository agentRepository;

    @Transactional(readOnly = true)
    public OptionsFiltresDTO options() {
        return new OptionsFiltresDTO(
                agentRepository.optionsCorps(),
                agentRepository.optionsGrades(),
                sansLibelle(agentRepository.categories()),
                agentRepository.optionsFiv(),
                agentRepository.optionsSoa(),
                sansLibelle(agentRepository.regions()),
                agentRepository.optionsMinisteres());
    }

    /** Catégories et régions n'ont pas de référentiel : le code seul. */
    private static List<OptionFiltre> sansLibelle(List<String> codes) {
        return codes.stream().map(code -> new OptionFiltre(code, null)).toList();
    }
}
