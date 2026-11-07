package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.CreateUtilisateurRequest;
import com.entreprise.gestion.dto.UpdateUtilisateurRequest;
import com.entreprise.gestion.dto.UtilisateurDetailDTO;
import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.*;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceTest {
    @Mock UtilisateurRepository utilisateurRepository;
    @Mock AgentRepository agentRepository;
    @Mock RoleRepository roleRepository;
    @Mock UtilisateurRoleRepository utilisateurRoleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;
    @InjectMocks UtilisateurService service;

    private final UserDetailsImpl actor = new UserDetailsImpl(Utilisateur.builder().id(1L).email("admin@x.mg").build());

    private static CreateUtilisateurRequest demande(String matricule) {
        CreateUtilisateurRequest r = new CreateUtilisateurRequest();
        r.setEmail("jean@x.mg"); r.setPassword("motdepasse1"); r.setMatriculeAgent(matricule);
        return r;
    }

    @Test void unAgentNAUnQuUnCompte() {
        when(utilisateurRepository.findByMatriculeAgent("123456"))
                .thenReturn(Optional.of(Utilisateur.builder().id(9L).email("autre@x.mg").build()));
        assertThatThrownBy(() -> service.create(demande("123456"), actor))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("AGENT_DEJA_LIE"));
    }

    @Test void leNomEstCopieDeLAgentQuandLeCompteNEnAPas() {
        when(agentRepository.findById("123456")).thenReturn(Optional.of(Agent.builder().matricule("123456").nom("Rakoto").prenoms("Jean").build()));
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(roleRepository.findByNomRole("ROLE_GESTIONNAIRE")).thenReturn(Optional.of(Role.builder().nomRole("ROLE_GESTIONNAIRE").build()));
        when(utilisateurRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UtilisateurDetailDTO dto = service.create(demande(" 123456 "), actor);

        assertThat(dto.matriculeAgent()).isEqualTo("123456");   // espaces retirés
        assertThat(dto.nom()).isEqualTo("Rakoto");
        assertThat(dto.prenom()).isEqualTo("Jean");
        assertThat(dto.agentLie().trouve()).isTrue();
        assertThat(dto.roles()).containsExactly("ROLE_GESTIONNAIRE");
    }

    @Test void unLienVersUnAgentAbsentEstAccepte() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(roleRepository.findByNomRole("ROLE_GESTIONNAIRE")).thenReturn(Optional.of(Role.builder().nomRole("ROLE_GESTIONNAIRE").build()));
        when(utilisateurRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UtilisateurDetailDTO dto = service.create(demande("777777"), actor);

        assertThat(dto.agentLie().trouve()).isFalse();
        assertThat(dto.agentLie().matricule()).isEqualTo("777777");
    }

    @Test void onNeRetirePasLeRoleDuDernierAdmin() {
        Utilisateur cible = Utilisateur.builder().id(5L).email("b@x.mg").build();
        cible.getUtilisateurRoles().add(UtilisateurRole.builder().utilisateur(cible).role(Role.builder().nomRole("ROLE_ADMIN").build()).build());
        when(utilisateurRepository.findByIdVisibleParApi(5L)).thenReturn(Optional.of(cible));
        when(utilisateurRepository.countByRoleAdminActifExcludingSysteme()).thenReturn(1L);

        assertThatThrownBy(() -> service.update(5L, new UpdateUtilisateurRequest(null, null, null, List.of("ROLE_GESTIONNAIRE"), null), actor))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("DERNIER_ADMIN"));
    }
}
