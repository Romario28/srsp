package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.ApprobationRequest;
import com.entreprise.gestion.dto.DemandeCompteDTO;
import com.entreprise.gestion.dto.DemandeCompteRequest;
import com.entreprise.gestion.dto.PerimetreDTO;
import com.entreprise.gestion.dto.RefusRequest;
import com.entreprise.gestion.entite.Role;
import com.entreprise.gestion.entite.StatutUtilisateur;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.entite.UtilisateurRole;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Ministere;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.RoleRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.repository.UtilisateurRoleRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.acces.ControleAcces;
import com.entreprise.gestion.service.acces.Perimetre;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DemandeCompteServiceTest {

    @Mock UtilisateurRepository utilisateurRepository;
    @Mock AgentRepository agentRepository;
    @Mock RoleRepository roleRepository;
    @Mock UtilisateurRoleRepository utilisateurRoleRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuditService auditService;
    @Mock ControleAcces controleAcces;
    @InjectMocks DemandeCompteService service;

    private UserDetailsImpl admin;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(service, "maxDemandesEnAttente", 200);
        Utilisateur uAdmin = Utilisateur.builder().id(1L).email("admin@x.mg").build();
        uAdmin.getUtilisateurRoles().add(UtilisateurRole.builder().utilisateur(uAdmin).role(Role.builder().nomRole("ROLE_ADMIN").build()).build());
        admin = new UserDetailsImpl(uAdmin);
    }

    private static DemandeCompteRequest demande(String matricule, String email, String profil) {
        return new DemandeCompteRequest(matricule, "Nom", "Prenom", email, "password123", profil);
    }

    private static Utilisateur utilisateurEnAttente(Long id, String email, String matricule, String profil) {
        Utilisateur u = Utilisateur.builder().id(id).email(email).matriculeAgent(matricule).statut(StatutUtilisateur.EN_ATTENTE).build();
        u.getUtilisateurRoles().add(UtilisateurRole.builder().utilisateur(u).role(Role.builder().nomRole(profil).build()).build());
        return u;
    }

    @Test
    void soumissionProfilAdminEstInterdite() {
        assertThatThrownBy(() -> service.soumettre(demande("123456", "test@x.mg", "ROLE_ADMIN"), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("PROFIL_INTERDIT"));
    }

    @Test
    void soumissionRefuseeSiEmailOuMatriculeExiste() {
        when(utilisateurRepository.existsByEmailIgnoreCase("deja@x.mg")).thenReturn(true);
        assertThatThrownBy(() -> service.soumettre(demande("123456", "deja@x.mg", "ROLE_AGENT"), "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("DEMANDE_IMPOSSIBLE"));
    }

    // [NOUVEAU] régression : la soumission réussie doit ajouter à la collection l'instance rendue par save(), pas l'original
    @Test
    void soumissionAjouteLInstanceRendueParLeDepot() {
        Role role = Role.builder().nomRole("ROLE_AGENT").build();
        UtilisateurRole geree = UtilisateurRole.builder().role(role).build();   // copie « gérée », distincte de l'original
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(utilisateurRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(roleRepository.findByNomRole("ROLE_AGENT")).thenReturn(Optional.of(role));
        when(utilisateurRoleRepository.save(any())).thenReturn(geree);

        service.soumettre(demande("123456", "nouveau@x.mg", "ROLE_AGENT"), "127.0.0.1");

        ArgumentCaptor<Utilisateur> capture = ArgumentCaptor.forClass(Utilisateur.class);
        verify(utilisateurRepository).save(capture.capture());
        assertThat(capture.getValue().getStatut()).isEqualTo(StatutUtilisateur.EN_ATTENTE);
        assertThat(capture.getValue().getUtilisateurRoles()).containsExactly(geree);
    }

    @Test
    void chefNeVoitQueLesDemandesDansSonPerimetre() {
        Utilisateur uChef = Utilisateur.builder().id(2L).email("chef@x.mg").build();
        uChef.getUtilisateurRoles().add(UtilisateurRole.builder().utilisateur(uChef).role(Role.builder().nomRole("ROLE_CHEF").build()).build());
        UserDetailsImpl chef = new UserDetailsImpl(uChef);
        when(controleAcces.perimetreDe(chef)).thenReturn(new Perimetre(false, Set.of("05"), Set.of(), Set.of()));

        Utilisateur d1 = utilisateurEnAttente(10L, "d1@x.mg", "111111", "ROLE_RH");
        Utilisateur d2 = utilisateurEnAttente(11L, "d2@x.mg", "222222", "ROLE_RH");
        Utilisateur d3 = utilisateurEnAttente(12L, "d3@x.mg", "333333", "ROLE_CHEF"); // chef ne doit pas voir les demandes chef
        when(utilisateurRepository.findByStatutOrderByDateCreationAsc(StatutUtilisateur.EN_ATTENTE))
                .thenReturn(List.of(d1, d2, d3));

        Agent a1 = Agent.builder().matricule("111111").ministere(Ministere.builder().code("05").build()).build();
        Agent a2 = Agent.builder().matricule("222222").ministere(Ministere.builder().code("06").build()).build();
        when(agentRepository.findAllById(any())).thenReturn(List.of(a1, a2));

        List<DemandeCompteDTO> res = service.lister(chef);
        assertThat(res).extracting(DemandeCompteDTO::email).containsExactly("d1@x.mg");
    }

    @Test
    void approbationPasseLeStatutAActifEtAppliquePerimetre() {
        Utilisateur u = utilisateurEnAttente(10L, "rh@x.mg", "111111", "ROLE_RH");
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(u));

        ApprobationRequest req = new ApprobationRequest(new PerimetreDTO(List.of("05"), List.of(), List.of()));
        service.approuver(10L, req, admin);

        assertThat(u.getStatut()).isEqualTo(StatutUtilisateur.ACTIF);
        verify(utilisateurRepository).save(u);
        verify(auditService).logAvecEntite(eq(admin.getUtilisateur()), eq("APPROBATION_COMPTE"), any(), isNull());
    }

    @Test
    void refusSupprimeLaDemandeEtLogAudit() {
        Utilisateur u = utilisateurEnAttente(10L, "rh@x.mg", "111111", "ROLE_RH");
        when(utilisateurRepository.findById(10L)).thenReturn(Optional.of(u));

        service.refuser(10L, new RefusRequest("Matricule non conforme"), admin);

        verify(utilisateurRepository).delete(u);
        verify(auditService).logAvecEntite(eq(admin.getUtilisateur()), eq("REFUS_COMPTE"), contains("Matricule non conforme"), isNull());
    }
}
