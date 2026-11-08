package com.entreprise.gestion.config;

import com.entreprise.gestion.security.JwtAuthFilter;
import com.entreprise.gestion.security.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsServiceImpl userDetailsService;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers("/h2-console/**").permitAll()
                        .requestMatchers("/error").permitAll()

                        // comptes : liste et fiche pour ADMIN et CHEF (le service limite le chef à son périmètre), le reste ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/utilisateurs", "/api/utilisateurs/*").hasAnyRole("ADMIN", "CHEF")
                        .requestMatchers("/api/utilisateurs/**").hasRole("ADMIN")
                        .requestMatchers("/api/audit/**").hasRole("ADMIN")
                        .requestMatchers("/api/imports/**").hasRole("ADMIN")

                        // fenêtres : lecture pour l'anticipation (elle pré-remplit les écrans), écriture ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/anticipation/configuration").hasAnyRole("ADMIN", "RH", "CHEF")
                        .requestMatchers("/api/anticipation/configuration/**").hasRole("ADMIN")

                        // actions sur un dossier : le personnel, et l'agent sur son propre dossier (le service tranche)
                        .requestMatchers(HttpMethod.POST, "/api/dossiers/*/demarrage", "/api/dossiers/*/sans-suite", "/api/dossiers/*/reouverture")
                                .hasAnyRole("ADMIN", "RH", "CHEF", "AGENT")

                        // anticipation, alertes, dossiers, tableau de bord, exports : ADMIN, RH, CHEF (le périmètre est imposé par les services)
                        .requestMatchers("/api/anticipation/**", "/api/alertes/**", "/api/dossiers/**",
                                "/api/dashboard/**", "/api/exports/**").hasAnyRole("ADMIN", "RH", "CHEF")

                        // espace personnel de l'agent
                        .requestMatchers("/api/mon-espace/**").hasRole("AGENT")
                        .anyRequest().authenticated()
                )
                .headers(h -> h.frameOptions(f -> f.disable()))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:[*]",
                "http://127.0.0.1:[*]",
                "http://192.168.*.*:[*]",
                "http://10.*.*.*:[*]",
                "http://172.*.*.*:[*]"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Content-Disposition"));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
