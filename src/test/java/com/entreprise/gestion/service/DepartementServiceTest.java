package com.entreprise.gestion.service;

import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.DepartementRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartementServiceTest {
    @Mock DepartementRepository departementRepository;
    @Mock UtilisateurRepository utilisateurRepository;
    @Mock AuditService auditService;
    @InjectMocks DepartementService service;

    private final UserDetailsImpl acteur = new UserDetailsImpl(Utilisateur.builder().id(1L).email("admin@x.mg").build());

    private void refuse(ThrowingCallable action, String code) {
        assertThatThrownBy(action).isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo(code));
    }

    @Test void creerNettoieLesEspacesEtTrace() {
        when(departementRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        assertThat(service.creer("  (SRSP)   Service  ", acteur).libelle()).isEqualTo("(SRSP) Service");
        verify(auditService).logAvecEntite(any(), eq("CREATE_DEPARTEMENT"), any(), isNull());
    }

    @Test void creerRefuseUnDoublonSansTenirCompteDeLaCasse() {
        when(departementRepository.existsByLibelleIgnoreCase("srsp")).thenReturn(true);
        refuse(() -> service.creer("srsp", acteur), "DEPARTEMENT_EXISTANT");
        verify(departementRepository, never()).save(any());
    }

    @Test void unLibelleVideEstRefuse() {
        refuse(() -> service.creer("   ", acteur), "LIBELLE_REQUIS");
        refuse(() -> service.creer(null, acteur), "LIBELLE_REQUIS");
    }

    @Test void modifierRefuseLeLibelleDUnAutreDepartement() {
        when(departementRepository.findById(3L)).thenReturn(Optional.of(Departement.builder().id(3L).libelle("A").build()));
        when(departementRepository.existsByLibelleIgnoreCaseAndIdNot("B", 3L)).thenReturn(true);
        refuse(() -> service.modifier(3L, "B", acteur), "DEPARTEMENT_EXISTANT");
        verify(departementRepository, never()).save(any());
    }

    @Test void modifierRenommeEtTrace() {
        Departement d = Departement.builder().id(3L).libelle("A").build();
        when(departementRepository.findById(3L)).thenReturn(Optional.of(d));
        assertThat(service.modifier(3L, " Nouveau ", acteur).libelle()).isEqualTo("Nouveau");
        assertThat(d.getLibelle()).isEqualTo("Nouveau");
        verify(auditService).logAvecEntite(any(), eq("UPDATE_DEPARTEMENT"), any(), isNull());
    }

    @Test void supprimerRefuseUnDepartementUtilise() {
        when(departementRepository.findById(3L)).thenReturn(Optional.of(Departement.builder().id(3L).libelle("A").build()));
        when(utilisateurRepository.countByDepartementId(3L)).thenReturn(2L);
        refuse(() -> service.supprimer(3L, acteur), "DEPARTEMENT_UTILISE");
        verify(departementRepository, never()).delete(any());
    }

    @Test void supprimerUnDepartementLibre() {
        Departement d = Departement.builder().id(3L).libelle("A").build();
        when(departementRepository.findById(3L)).thenReturn(Optional.of(d));
        when(utilisateurRepository.countByDepartementId(3L)).thenReturn(0L);
        service.supprimer(3L, acteur);
        verify(departementRepository).delete(d);
        verify(auditService).logAvecEntite(any(), eq("DELETE_DEPARTEMENT"), any(), isNull());
    }
}
