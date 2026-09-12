package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.Session;
import com.entreprise.gestion.entite.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    Optional<Session> findByToken(String token);

    List<Session> findByUtilisateurAndActif(Utilisateur utilisateur, boolean actif);

    @Modifying
    @Transactional
    @Query("UPDATE Session s SET s.actif = false WHERE s.utilisateur.id = :utilisateurId")
    void deactivateAllByUtilisateurId(Long utilisateurId);
}
