package com.entreprise.gestion.security;

import com.entreprise.gestion.entite.*;
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
 *
 * Usage dans les controllers :
 *
 *   // Injection directe via @AuthenticationPrincipal (recommandé)
 *   @GetMapping("/profil")
 *   public ResponseEntity<?> getProfil(
 *           @AuthenticationPrincipal UserDetailsImpl principal) {
 *       Long id          = principal.getId();
 *       Utilisateur u    = principal.getUtilisateur();
 *       Employe emp      = principal.getEmploye();     // peut être null
 *       String nomGroupe = principal.getNomGroupe();   // peut être null
 *   }
 *
 *   // Ou via Authentication (si @AuthenticationPrincipal n'est pas disponible)
 *   UserDetailsImpl principal = (UserDetailsImpl) auth.getPrincipal();
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

    /**
     * Pourrait vérifier une date d'expiration de compte (ex: comptes temporaires).
     * Pour l'instant : toujours vrai.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Pourrait retourner false après N tentatives échouées (brute-force protection).
     * Pour l'instant : toujours vrai.
     */
    @Override
    public boolean isAccountNonLocked() {
        return utilisateur.getStatut() != StatutUtilisateur.SUSPENDU;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    // ── Raccourcis pratiques pour les controllers et services ────────────────
    // Évite de faire principal.getUtilisateur().getXxx() partout.

    public Long getId() {
        return utilisateur.getId();
    }

    public String getEmail() {
        return utilisateur.getEmail();
    }

    public StatutUtilisateur getStatut() {
        return utilisateur.getStatut();
    }

    /**
     * Retourne le nom complet de l'employé lié, ou l'email si aucun employé.
     * Utilisé pour les logs d'audit et les réponses REST.
     */
    public String getNomComplet() {
        Employe emp = utilisateur.getEmploye();
        return emp != null ? emp.getPrenom() + " " + emp.getNom() : utilisateur.getEmail();
    }

    /**
     * Retourne l'employé lié, ou null si c'est un compte système (ex: admin pur).
     */
    public Employe getEmploye() {
        return utilisateur.getEmploye();
    }



    public String getNomDepartement() {
        Employe emp = utilisateur.getEmploye();
        if (emp == null || emp.getDepartement() == null) return null;
        Departement d = emp.getDepartement();
        return d.getNomDepartement();
    }


    /**
     * Retourne la date de dernière connexion, utile pour les réponses /me.
     */
    public LocalDateTime getDateDerniereConnexion() {
        return utilisateur.getDateDerniereConnexion();
    }

    /**
     * Vérifie si le principal possède un rôle donné.
     * Exemple : principal.hasRole("ADMIN")
     */
    public boolean hasRole(String role) {
        return authorities.stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
