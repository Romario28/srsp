package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.CreateUtilisateurRequest;
import com.entreprise.gestion.dto.UpdateUtilisateurRequest;
import com.entreprise.gestion.dto.UtilisateurDTO;
import com.entreprise.gestion.dto.UtilisateurDetailDTO;
import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Ministere;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.*;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.acces.ControleAcces;
import com.entreprise.gestion.service.acces.Perimetre;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    @Mock ControleAcces controleAcces;
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
        when(roleRepository.findByNomRole("ROLE_AGENT")).thenReturn(Optional.of(Role.builder().nomRole("ROLE_AGENT").build()));
        when(utilisateurRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        UtilisateurDetailDTO dto = service.create(demande(" 123456 "), actor);

        assertThat(dto.matriculeAgent()).isEqualTo("123456");
        assertThat(dto.nom()).isEqualTo("Rakoto");
        assertThat(dto.prenom()).isEqualTo("Jean");
        assertThat(dto.agentLie().trouve()).isTrue();
        assertThat(dto.roles()).containsExactly("ROLE_AGENT");
    }

    @Test void unLienVersUnAgentAbsentEstAccepte() {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(roleRepository.findByNomRole("ROLE_AGENT")).thenReturn(Optional.of(Role.builder().nomRole("ROLE_AGENT").build()));
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

        assertThatThrownBy(() -> service.update(5L, new UpdateUtilisateurRequest(null, null, null, List.of("ROLE_RH"), null, false), actor))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("DERNIER_ADMIN"));
    }

    private void preparerCreation(String role) {
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(roleRepository.findByNomRole(role)).thenReturn(Optional.of(Role.builder().nomRole(role).build()));
        when(utilisateurRoleRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test void unCompteADesProfilsUniqueEtConnu() {
        CreateUtilisateurRequest deux = demande("123456"); deux.setRoles(List.of("ROLE_RH", "ROLE_ADMIN"));
        CreateUtilisateurRequest ancien = demande("123456"); ancien.setRoles(List.of("ROLE_EMPLOYE"));
        for (CreateUtilisateurRequest r : List.of(deux, ancien))
            assertThatThrownBy(() -> service.create(r, actor))
                    .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("PROFIL_INVALIDE"));
    }

    @Test void unCompteAgentExigeUnMatricule() {
        assertThatThrownBy(() -> service.create(demande(null), actor))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("AGENT_SANS_MATRICULE"));
    }

    @Test void leDroitDAgirEstAccordeSeulementAUnAgent() {
        preparerCreation("ROLE_AGENT");
        CreateUtilisateurRequest agent = demande("123456"); agent.setAgirSurSonDossier(true);
        assertThat(service.create(agent, actor).agirSurSonDossier()).isTrue();
    }

    @Test void leDroitDAgirEstIgnorePourUnProfilRh() {
        preparerCreation("ROLE_RH");
        CreateUtilisateurRequest rh = demande(null); rh.setRoles(List.of("ROLE_RH")); rh.setAgirSurSonDossier(true);
        assertThat(service.create(rh, actor).agirSurSonDossier()).isFalse();
    }

    private static Utilisateur compte(long id, String email, String role, String matricule, String ministere) {
        Utilisateur u = Utilisateur.builder().id(id).email(email).matriculeAgent(matricule).build();
        u.getUtilisateurRoles().add(UtilisateurRole.builder().utilisateur(u).role(Role.builder().nomRole(role).build()).build());
        if (ministere != null) u.getPerimetres().add(PerimetreUtilisateur.builder().utilisateur(u)
                .dimension(DimensionPerimetre.MINISTERE).code(ministere).build());
        return u;
    }
    private static Agent agentDuMinistere(String matricule, String ministere) {
        return Agent.builder().matricule(matricule).ministere(Ministere.builder().code(ministere).build()).build();
    }

    @Test void unChefNeVoitQueLesComptesRhOuAgentDeSonPerimetre() {
        UserDetailsImpl chef = new UserDetailsImpl(compte(2L, "chef@x.mg", "ROLE_CHEF", null, "05"));
        when(controleAcces.perimetreDe(chef)).thenReturn(new Perimetre(false, Set.of("05"), Set.of(), Set.of()));
        when(utilisateurRepository.findAll(any(Sort.class))).thenReturn(List.of(
                chef.getUtilisateur(),
                compte(10L, "rh-dedans@x.mg", "ROLE_RH", null, "05"),
                compte(11L, "rh-dehors@x.mg", "ROLE_RH", null, "06"),
                compte(12L, "rh-sans-valeur@x.mg", "ROLE_RH", null, null),
                compte(13L, "agent-dedans@x.mg", "ROLE_AGENT", "111111", null),
                compte(14L, "agent-dehors@x.mg", "ROLE_AGENT", "222222", null),
                compte(15L, "agent-inconnu@x.mg", "ROLE_AGENT", "333333", null),
                compte(16L, "admin@x.mg", "ROLE_ADMIN", null, null),
                compte(17L, "autre-chef@x.mg", "ROLE_CHEF", null, "05")));
        when(agentRepository.findAllById(any())).thenReturn(List.of(agentDuMinistere("111111", "05"), agentDuMinistere("222222", "06")));

        Page<UtilisateurDTO> page = service.findAll(PageRequest.of(0, 20), chef);

        assertThat(page.getContent()).extracting(UtilisateurDTO::email).containsExactly("rh-dedans@x.mg", "agent-dedans@x.mg");
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
