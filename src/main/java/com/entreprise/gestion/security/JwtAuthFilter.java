package com.entreprise.gestion.security;

import com.entreprise.gestion.entite.Session;
import com.entreprise.gestion.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre JWT — inchangé par la migration.
 *
 * loadUserByUsername() retourne maintenant un UserDetailsImpl,
 * l'interface UserDetails. Le principal injecté dans le SecurityContext
 * mais le filtre n'a pas besoin de le savoir : il travaille avec
 * est automatiquement un UserDetailsImpl, accessible ensuite via
 * @AuthenticationPrincipal dans les controllers.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService        jwtService;
    private final UserDetailsService userDetailsService;
    private final SessionRepository sessionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String email;

        try {
            email = jwtService.extractUsername(jwt);
        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }


        if (email != null
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // loadUserByUsername retourne maintenant un UserDetailsImpl
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                boolean tokenValide  = jwtService.isTokenValid(jwt, userDetails);
                boolean sessionActive = sessionRepository
                        .findByToken(jwt)
                        .map(Session::isActif)
                        .orElse(false);

                if (tokenValide && sessionActive) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                }
            } catch (UsernameNotFoundException e) {
                // Compte désactivé, token expiré, etc. — pas d'authentification
//
                logger.warn("Token valide mais utilisateur introuvable : {}" + email, e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
