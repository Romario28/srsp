package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.LoginRequest;
import com.entreprise.gestion.dto.LoginResponse;
import com.entreprise.gestion.entite.Session;
import com.entreprise.gestion.entite.StatutUtilisateur;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.SessionRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.security.JwtService;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager  authManager;
    private final UserDetailsService     userDetailsService;
    private final UtilisateurRepository  utilisateurRepository;
    private final SessionRepository      sessionRepository;
    private final JwtService             jwtService;
    private final AuditService           auditService;
    private final PasswordEncoder        passwordEncoder;   // [NOUVEAU]

    @Transactional
    public LoginResponse login(LoginRequest req, String ip) {

        // 1. Valide email + mot de passe
        Authentication authentication;
        try {
            authentication = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (DisabledException | LockedException e) {
            throw refusCompteInactif(req, e);
        }

        // 2. loadUserByUsername retourne maintenant un UserDetailsImpl
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

        Utilisateur utilisateur = principal.getUtilisateur();

        // 3. Mettre à jour la date de dernière connexion
        utilisateur.setDateDerniereConnexion(LocalDateTime.now());
        utilisateurRepository.save(utilisateur);

        // 4. Générer le JWT
        String token = jwtService.generateToken(principal);

        // 5. Persister la session
        Session session = Session.builder()
                .utilisateur(utilisateur)
                .token(token)
                .adresseIp(ip)
                .dateExpiration(LocalDateTime.now().plusDays(1))
                .actif(true)
                .build();
        sessionRepository.save(session);

        // 6. Log audit — on passe l'entité directement depuis le principal
        auditService.logAvecEntite(utilisateur, "LOGIN",
                "Connexion depuis " + ip, ip);

        // 7. Construire la réponse
        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return new LoginResponse(
                token,
                utilisateur.getEmail(),
                principal.getNomComplet(),   // méthode du UserDetailsImpl
                roles,
                utilisateur.getStatut().name()
        );
    }

    // [NOUVEAU] ↓
    /**
     * Compte inactif : l'état n'est expliqué qu'avec le bon mot de passe. Sans lui, la réponse est celle d'un mauvais
     * mot de passe, comme pour un e-mail inconnu.
     */
    private RuntimeException refusCompteInactif(LoginRequest req, AuthenticationException origine) {
        Utilisateur u = utilisateurRepository.findByEmail(req.getEmail()).orElse(null);
        if (u == null || !passwordEncoder.matches(req.getPassword(), u.getMotDePasseHash())) {
            return new BadCredentialsException("Identifiants invalides");
        }
        if (u.getStatut() == StatutUtilisateur.EN_ATTENTE) {
            return new BusinessException("COMPTE_EN_ATTENTE",
                    "Votre demande de compte est en attente de validation. Vous pourrez vous connecter dès qu'elle aura été approuvée.");
        }
        return origine;   // compte désactivé ou suspendu : message habituel
    }
    // [NOUVEAU] ↑

    @Transactional
    public void logout(String token, String email) {
        sessionRepository.findByToken(token).ifPresent(s -> {
            s.setActif(false);
            sessionRepository.save(s);
        });
        auditService.log(email, "LOGOUT", "Déconnexion", null);
    }
}
