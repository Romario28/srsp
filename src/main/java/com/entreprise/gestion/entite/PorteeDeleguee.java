package com.entreprise.gestion.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

/**
 * Délégation explicite de portée : accorde à un UTILISATEUR une visibilité
 * (lecture ou lecture/écriture) sur un DEPARTEMENT et son sous-arbre
 * (résolu via Departement.chemin), en plus de sa portée structurelle
 * (chef) ou globale (ROLE_ADMIN) habituelle.
 *
 * Cas d'usage : intérim pendant un congé, assistant(e) qui gère pour le
 * compte d'un chef, auditeur, consultant externe — et aussi l'autorité RH
 * elle-même (permanente : date_fin = null, type_acces = LECTURE_ECRITURE),
 * qu'elle porte sur un département précis ou, via le département racine,
 * sur l'entreprise entière. Voir Departement.java pour le raisonnement
 * détaillé sur pourquoi RH n'a pas sa propre colonne structurelle.
 *
 * date_debut est fournie explicitement (pas de @PrePersist automatique) :
 * elle peut être future pour planifier une délégation à l'avance.
 * date_fin nulle = délégation permanente (jusqu'à révocation manuelle,
 * qui se fait en positionnant date_fin plutôt qu'en supprimant la ligne —
 * conserve la traçabilité, cohérent avec l'esprit de LogAudit).
 */
@Entity
@Table(
        name = "portee_deleguee",
        schema = "gestion_utilisateurs",
        indexes = {
                @Index(name = "idx_portee_utilisateur", columnList = "id_utilisateur"),
                @Index(name = "idx_portee_departement", columnList = "id_departement")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PorteeDeleguee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_portee")
    private Long id;

    // À qui on délègue une visibilité
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_utilisateur",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_portee_deleguee_utilisateur")
    )
    private Utilisateur utilisateur;

    // Sur quel département (+ son sous-arbre, via Departement.chemin)
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_departement",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_portee_deleguee_departement")
    )
    private Departement departement;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type_acces", nullable = false, length = 20)
    private TypeAcces typeAcces;

    @NotNull
    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    // NULLABLE : délégation permanente si vide, temporaire sinon
    @Column(name = "date_fin")
    private LocalDate dateFin;

    // Traçabilité : qui a accordé cet accès
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "accorde_par",
            nullable = false,
            foreignKey = @ForeignKey(name = "FK_portee_deleguee_accorde_par")
    )
    private Utilisateur accordePar;

    /**
     * Une délégation est active à une date donnée si celle-ci tombe dans
     * [date_debut, date_fin] (date_fin nulle = pas de limite supérieure).
     */
    public boolean estActive(LocalDate aDate) {
        boolean apresDebut = !aDate.isBefore(dateDebut);
        boolean avantFin   = (dateFin == null) || !aDate.isAfter(dateFin);
        return apresDebut && avantFin;
    }
}