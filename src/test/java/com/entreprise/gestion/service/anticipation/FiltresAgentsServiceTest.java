package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.dto.anticipation.OptionFiltre;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.service.acces.Perimetre;
import com.entreprise.gestion.service.acces.PerimetreCourant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FiltresAgentsServiceTest {
    @Mock AgentRepository agentRepository;
    @Mock PerimetreCourant perimetreCourant;
    @InjectMocks FiltresAgentsService service;

    private void ministeres() {
        when(agentRepository.optionsMinisteres()).thenReturn(List.of(new OptionFiltre("05", "A"), new OptionFiltre("06", "B")));
    }

    @Test void unPerimetreRestreintLimiteLesMinisteresProposes() {
        when(perimetreCourant.get()).thenReturn(new Perimetre(false, Set.of("05"), Set.of(), Set.of()));
        ministeres();
        assertThat(service.options().ministeres()).extracting(OptionFiltre::code).containsExactly("05");
    }

    @Test void unAccesTotalVoitToutes() {
        when(perimetreCourant.get()).thenReturn(Perimetre.tout());
        ministeres();
        assertThat(service.options().ministeres()).extracting(OptionFiltre::code).containsExactly("05", "06");
    }

    @Test void unCompteSansAucuneValeurNeVoitAucuneOption() {
        when(perimetreCourant.get()).thenReturn(Perimetre.aucun());
        assertThat(service.options().ministeres()).isEmpty();
    }
}
