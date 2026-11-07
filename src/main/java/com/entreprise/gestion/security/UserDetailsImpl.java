package com.entreprise.gestion.security;

import com.entreprise.gestion.entite.StatutUtilisateur;
import com.entreprise.gestion.entite.Utilisateur;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implémentation personnalisée de UserDetails.
 *
 * Encapsule l'entité Utilisateur et l'expose directement depuis le SecurityContext,
 * évitant les requêtes BDD répétées dans les controllers et services.
 */
@Getter
public class UserDetailsImpl implements UserDetails {

    // ── Entité complète accessible partout ───────────────────────────────────
    private final Utilisateur utilisateur;

    // ── Autorités calculées une seule fois à la construction ─────────────────
    private final List<GrantedAuthority> authorities;

    // ────────────────────────────────────────────────────────────────────────

    public UserDetailsImpl(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
        this.authorities = utilisateur.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getNomRole()))
                .collect(Collectors.toList());
    }

    // ── Contrat UserDetails ──────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return utilisateur.getEmail();
    }

    @Override
    public String getPassword() {
        return utilisateur.getMotDePasseHash();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /**
     * Logique métier lisible — pas un booléen noyé dans un constructeur à 6 params.
     * Un compte est actif seulement si son statut == ACTIF.
     */
    @Override
    public boolean isEnabled() {
        return utilisateur.getStatut() == StatutUtilisateur.ACTIF;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return utilisateur.getStatut() != StatutUtilisateur.SUSPENDU;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ── Raccourcis pratiques pour les controllers et services ────────────────

    public Long getId() {
        return utilisateur.getId();
    }

    public String getEmail() {
        return utilisateur.getEmail();
    }

    public StatutUtilisateur getStatut() {
        return utilisateur.getStatut();
    }

    public String getNomComplet() {
        String nom = utilisateur.nomComplet();
        return nom.isEmpty() ? utilisateur.getEmail() : nom;
    }

    public String getMatriculeAgent() {
        return utilisateur.getMatriculeAgent();
    }

    public LocalDateTime getDateDerniereConnexion() {
        return utilisateur.getDateDerniereConnexion();
    }

    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
