package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.LoginRequest;
import com.entreprise.gestion.dto.LoginResponse;
import com.entreprise.gestion.entite.Session;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.repository.SessionRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.security.JwtService;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
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

    @Transactional
    public LoginResponse login(LoginRequest req, String ip) {

        // 1. Valide email + mot de passe
        Authentication authentication = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        //authManager.authenticate(new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));

        // 2. loadUserByUsername retourne maintenant un UserDetailsImpl
        //    Cast direct — plus besoin de recharger l'entité séparément
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();
        //UserDetailsImpl principal = (UserDetailsImpl) userDetailsService.loadUserByUsername(req.getEmail());


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

    @Transactional
    public void logout(String token, String email) {
        sessionRepository.findByToken(token).ifPresent(s -> {
            s.setActif(false);
            sessionRepository.save(s);
        });
        auditService.log(email, "LOGOUT", "Déconnexion", null);
    }
}
