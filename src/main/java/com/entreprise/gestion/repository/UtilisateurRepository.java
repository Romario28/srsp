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

    // [NOUVEAU] ↓
    boolean existsByEmailIgnoreCase(String email);
    long countByStatut(StatutUtilisateur statut);
    List<Utilisateur> findByStatutOrderByDateCreationAsc(StatutUtilisateur statut);
    // [NOUVEAU] ↑

    // [NOUVEAU] sert à refuser la suppression d'un département utilisé
    long countByDepartementId(Long idDepartement);

    Optional<Utilisateur> findByMatriculeAgent(String matriculeAgent);

    /** Liste API : exclut les comptes système et les demandes en attente (elles ont leur écran). */
    Page<Utilisateur> findByCompteSystemeFalseAndStatutNot(StatutUtilisateur statut, Pageable pageable);

    /** Lecture unitaire API : un compte système ou une demande en attente n'est ni consultable ni modifiable par l'écran des comptes. */
    @Query("SELECT u FROM Utilisateur u WHERE u.id = :id AND u.compteSysteme = false "
         + "AND u.statut <> com.entreprise.gestion.entite.StatutUtilisateur.EN_ATTENTE")
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
        WHERE u.email = :email
        """)
    Optional<Utilisateur> findByEmailWithRoles(@Param("email") String email);

}
