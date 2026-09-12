package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.StatutUtilisateur;
import com.entreprise.gestion.entite.Utilisateur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Liste API : exclut structurellement les comptes système. */
    Page<Utilisateur> findByCompteSystemeFalse(Pageable pageable);

    /** Lecture unitaire API : un compte système est "introuvable" côté API. */
    @Query("SELECT u FROM Utilisateur u WHERE u.id = :id AND u.compteSysteme = false")
    Optional<Utilisateur> findByIdVisibleParApi(@Param("id") Long id);

    /** Compte les ADMIN actifs hors comptes système — garantit qu'on ne suspend jamais le dernier. */
    @Query("""
        SELECT COUNT(DISTINCT u) FROM Utilisateur u
        JOIN u.utilisateurRoles ur JOIN ur.role r
        WHERE r.nomRole = 'ROLE_ADMIN'
          AND u.statut = com.entreprise.gestion.entite.StatutUtilisateur.ACTIF
          AND u.compteSysteme = false
        """)
    long countByRoleAdminActifExcludingSysteme();



    @Query("""
        SELECT u FROM Utilisateur u
        LEFT JOIN FETCH u.utilisateurRoles ur
        LEFT JOIN FETCH ur.role
        LEFT JOIN FETCH u.employe e
        LEFT JOIN FETCH e.departement
        WHERE u.email = :email
        """)
    Optional<Utilisateur> findByEmailWithRolesAndRelations(@Param("email") String email);



}
