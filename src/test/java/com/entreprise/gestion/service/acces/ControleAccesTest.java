package com.entreprise.gestion.service.acces;

import com.entreprise.gestion.config.anticipation.DossierProperties;
import com.entreprise.gestion.entite.*;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Ministere;
import com.entreprise.gestion.entite.anticipation.Soa;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.PerimetreUtilisateurRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ControleAccesTest {
    private final PerimetreUtilisateurRepository repo = mock(PerimetreUtilisateurRepository.class);
    private final DossierProperties proprietes = new DossierProperties();
    private final ControleAcces acces = new ControleAcces(repo, proprietes);

    private static UserDetailsImpl acteur(long id, String role, String matricule) {
        Utilisateur u = Utilisateur.builder().id(id).email("a" + id + "@x.mg").matriculeAgent(matricule).build();
        u.getUtilisateurRoles().add(UtilisateurRole.builder().utilisateur(u).role(Role.builder().nomRole(role).build()).build());
        return new UserDetailsImpl(u);
    }
    private static PerimetreUtilisateur p(DimensionPerimetre d, String code) {
        return PerimetreUtilisateur.builder().dimension(d).code(code).build();
    }
    private static Agent agent(String matricule, String ministere, String soa) {
        Agent.AgentBuilder b = Agent.builder().matricule(matricule);
        if (ministere != null) b.ministere(Ministere.builder().code(ministere).build());
        if (soa != null) b.soa(Soa.builder().code(soa).build());
        return b.build();
    }

    @Test void unGestionnaireAgitDansSonPerimetre() {
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of(p(DimensionPerimetre.MINISTERE, "05")));
        assertThatCode(() -> acces.verifierActionSurDossier(acteur(2, "ROLE_RH", null), agent("111111", "05", null), "démarrer"))
                .doesNotThrowAnyException();
    }
    @Test void horsPerimetreEstRefuse() {
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of(p(DimensionPerimetre.MINISTERE, "05")));
        assertThatThrownBy(() -> acces.verifierActionSurDossier(acteur(2, "ROLE_RH", null), agent("111111", "06", null), "démarrer"))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test void sansAucuneValeurOnNeVoitRien() {
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of());
        assertThatThrownBy(() -> acces.verifierActionSurDossier(acteur(2, "ROLE_RH", null), agent("111111", "05", null), "rouvrir"))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test void uneDimensionVideNeRestreintPas() {
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of(p(DimensionPerimetre.MINISTERE, "05")));
        assertThat(acces.perimetreDe(acteur(2, "ROLE_RH", null)).autorise(agent("111111", "05", null))).isTrue();
    }
    @Test void unAgentSansLaValeurDemandeeEstExclu() {
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of(p(DimensionPerimetre.SOA, "S1")));
        assertThat(acces.perimetreDe(acteur(2, "ROLE_RH", null)).autorise(agent("111111", "05", null))).isFalse();
    }
    @Test void leLienNOuvreAucunDroit() {
        // compte lié à l'agent 123456, mais sans aucune valeur de périmètre : il ne voit rien, même son propre agent
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of());
        assertThatThrownBy(() -> acces.verifierActionSurDossier(acteur(2, "ROLE_RH", "123456"), agent("123456", "05", null), "démarrer"))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test void unCompteLieNePeutPasAgirSurSonPropreDossier() {
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of(p(DimensionPerimetre.MINISTERE, "05")));
        assertThatThrownBy(() -> acces.verifierActionSurDossier(acteur(2, "ROLE_RH", "123456"), agent("123456", "05", null), "démarrer"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("DOSSIER_PROPRE"));
    }
    @Test void unCompteLieAgitSurLesAutresAgents() {
        when(repo.findByUtilisateurId(2L)).thenReturn(List.of(p(DimensionPerimetre.MINISTERE, "05")));
        assertThatCode(() -> acces.verifierActionSurDossier(acteur(2, "ROLE_RH", "123456"), agent("999999", "05", null), "démarrer"))
                .doesNotThrowAnyException();
    }
    @Test void lAdminLieEstSoumisParDefaut() {
        assertThatThrownBy(() -> acces.verifierActionSurDossier(acteur(1, "ROLE_ADMIN", "123456"), agent("123456", null, null), "démarrer"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("DOSSIER_PROPRE"));
    }
    @Test void unRoleExempteEstDispense() {
        proprietes.setRolesExemptesConflit(Set.of("ROLE_ADMIN"));
        assertThatCode(() -> acces.verifierActionSurDossier(acteur(1, "ROLE_ADMIN", "123456"), agent("123456", null, null), "démarrer"))
                .doesNotThrowAnyException();
        assertThat(acces.exempteDuConflit(acteur(1, "ROLE_ADMIN", null))).isTrue();
        assertThat(acces.exempteDuConflit(acteur(2, "ROLE_RH", null))).isFalse();
    }

    private static UserDetailsImpl agent(long id, String matricule, boolean droit) {
        Utilisateur u = Utilisateur.builder().id(id).email("a" + id + "@x.mg").matriculeAgent(matricule).agirSurSonDossier(droit).build();
        u.getUtilisateurRoles().add(UtilisateurRole.builder().utilisateur(u).role(Role.builder().nomRole("ROLE_AGENT").build()).build());
        return new UserDetailsImpl(u);
    }

    @Test void unAgentEnLectureSeuleNeFaitRienSurSonDossier() {
        assertThatThrownBy(() -> acces.verifierActionSurDossier(agent(3, "123456", false), agent("123456", "05", null), "démarrer"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("LECTURE_SEULE"));
    }
    @Test void unAgentAvecLeDroitAgitSurSonPropreDossier() {
        assertThatCode(() -> acces.verifierActionSurDossier(agent(3, "123456", true), agent("123456", "05", null), "démarrer"))
                .doesNotThrowAnyException();
    }
    @Test void unAgentNAgitJamaisSurLeDossierDUnAutreMemeAvecLeDroit() {
        assertThatThrownBy(() -> acces.verifierActionSurDossier(agent(3, "123456", true), agent("999999", "05", null), "démarrer"))
                .isInstanceOf(AccessDeniedException.class);
    }
    @Test void unAgentNAAucunPerimetre() {
        assertThat(acces.perimetreDe(agent(3, "123456", true)).estVide()).isTrue();
    }
}
