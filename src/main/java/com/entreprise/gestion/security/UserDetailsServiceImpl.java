package com.entreprise.gestion.security;

import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        Utilisateur utilisateur = utilisateurRepository
                .findByEmailWithRole(email)   // [MODIFIÉ] était : findByEmailWithRoles
                .orElseThrow(() ->
                        new UsernameNotFoundException("Utilisateur introuvable : " + email));

        /*
         * On ne vérifie plus le statut ici avec une exception DisabledException.
         * Spring Security appelle isEnabled() sur le UserDetails retourné
         * et lève lui-même DisabledException si false.
         * La logique métier est dans UserDetailsImpl.isEnabled().
         */
        return new UserDetailsImpl(utilisateur);
    }
}
