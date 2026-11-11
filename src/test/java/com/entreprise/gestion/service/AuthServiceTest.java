package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.LoginRequest;
import com.entreprise.gestion.dto.LoginResponse;
import com.entreprise.gestion.entite.StatutUtilisateur;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.SessionRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.security.JwtService;
import com.entreprise.gestion.security.UserDetailsImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock AuthenticationManager authManager;
    @Mock UtilisateurRepository utilisateurRepository;
    @Mock SessionRepository sessionRepository;
    @Mock JwtService jwtService;
    @Mock AuditService auditService;
    @Mock PasswordEncoder passwordEncoder;
    @InjectMocks AuthService authService;

    @Test
    void loginSuccesQuandActif() {
        LoginRequest req = new LoginRequest("user@x.mg", "motdepasse");
        Utilisateur u = Utilisateur.builder().id(1L).email("user@x.mg").statut(StatutUtilisateur.ACTIF).build();
        UserDetailsImpl principal = new UserDetailsImpl(u);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        when(authManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtService.generateToken(principal)).thenReturn("token123");

        LoginResponse resp = authService.login(req, "127.0.0.1");

        assertThat(resp.getToken()).isEqualTo("token123");
        assertThat(resp.getEmail()).isEqualTo("user@x.mg");
    }

    @Test
    void compteEnAttenteAvecMauvaisMotDePasseRenvoieBadCredentials() {
        LoginRequest req = new LoginRequest("enattente@x.mg", "fauxmdp");
        when(authManager.authenticate(any())).thenThrow(new DisabledException("disabled"));
        Utilisateur u = Utilisateur.builder().email("enattente@x.mg").motDePasseHash("hash")
                .statut(StatutUtilisateur.EN_ATTENTE).build();
        when(utilisateurRepository.findByEmail("enattente@x.mg")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("fauxmdp", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void compteEnAttenteAvecBonMotDePasseReveleCompteEnAttente() {
        LoginRequest req = new LoginRequest("enattente@x.mg", "bonmdp");
        when(authManager.authenticate(any())).thenThrow(new DisabledException("disabled"));
        Utilisateur u = Utilisateur.builder().email("enattente@x.mg").motDePasseHash("hash")
                .statut(StatutUtilisateur.EN_ATTENTE).build();
        when(utilisateurRepository.findByEmail("enattente@x.mg")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("bonmdp", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.getCode()).isEqualTo("COMPTE_EN_ATTENTE"));
    }

    @Test
    void compteDesactiveAvecBonMotDePasseRenvoieDisabledException() {
        LoginRequest req = new LoginRequest("desactive@x.mg", "bonmdp");
        when(authManager.authenticate(any())).thenThrow(new DisabledException("disabled"));
        Utilisateur u = Utilisateur.builder().email("desactive@x.mg").motDePasseHash("hash")
                .statut(StatutUtilisateur.DESACTIVE).build();
        when(utilisateurRepository.findByEmail("desactive@x.mg")).thenReturn(Optional.of(u));
        when(passwordEncoder.matches("bonmdp", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
                .isInstanceOf(DisabledException.class);
    }
}
