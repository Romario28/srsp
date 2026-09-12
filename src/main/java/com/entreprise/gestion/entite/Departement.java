package com.entreprise.gestion.entite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Département de l'entreprise, organisé en hiérarchie arborescente.
 *
 * Une seule tête d'autorité structurelle, dérivée automatiquement (aucun
 * rôle Spring Security dédié n'est nécessaire) :
 * ┌───────────────────────────────────────────────────────────────────────┐
 * │  chef (OneToOne, UNIQUE) → autorité générale sur le sous-arbre         │
 * └───────────────────────────────────────────────────────────────────────┘
 *
 * L'autorité RH n'est PAS une deuxième tête structurelle ici (contrairement
 * à une version antérieure de ce fichier) : "avoir une autorité RH dans ce
 * système" signifie concrètement "pouvoir consulter/modifier les fiches
 * employé d'un sous-arbre" — c'est intrinsèquement un ACCÈS, pas un fait
 * d'organigramme indépendant du système (contrairement à `chef`, qui existe
 * même si la personne ne se connecte jamais). Ce cas est donc couvert par
 * PORTEE_DELEGUEE avec date_fin = NULL (délégation permanente, LECTURE_ECRITURE)
 * — que ce soit sur un département précis ou, via le département racine
 * + chemin, sur l'entreprise entière. Pas de deuxième colonne nécessaire.
 *
 * `chef` utilise le mécanisme de résolution de sous-arbre (`chemin`, chemin
 * matérialisé du type "/1/4/9/") — le même que celui utilisé par
 * PORTEE_DELEGUEE pour résoudre son propre sous-arbre délégué.
 *
 * ATTENTION - dépendance circulaire Departement ↔ Employe (chef) :
 * un département "doit avoir" un chef ET un employé "doit appartenir" à un
 * département. En base, id_chef reste NULLABLE (impossible de satisfaire
 * les deux sens de la relation en NOT NULL à la création de la toute
 * première ligne). Flux de création recommandé :
 *   1) créer le département SANS chef
 *   2) rattacher les employés à ce département (obligatoire, sans problème)
 *   3) assigner le chef une fois qu'il existe parmi les employés
 *
 * `chemin` et `niveau` ne sont PAS calculés automatiquement par cette entité :
 *  - chemin dépend de l'id auto-généré (donc calculable seulement APRÈS le
 *    premier save) et de la hiérarchie complète des parents — ce sera le
 *    rôle d'un futur DepartementService/PorteeService.
 *  - niveau est un simple libellé informatif (ex. "Direction Générale",
 *    "Direction", "Service", "Division"), sans contrainte en base — pure
 *    convention d'usage, sans effet sur le mécanisme de portée lui-même.
 */
@Entity
@Table(
        name = "departement",
        schema = "gestion_utilisateurs",
        indexes = {
                @Index(name = "idx_departement_parent", columnList = "id_departement_parent"),
                @Index(name = "idx_departement_chemin",  columnList = "chemin")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Departement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departement")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column(name = "nom_departement", nullable = false, unique = true, length = 100)
    private String nomDepartement;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @Size(max = 50)
    @Column(name = "niveau", length = 50)
    private String niveau;

    // Chemin matérialisé (ex. "/1/4/9/") pour résoudre le sous-arbre via
    // `chemin LIKE '/1/4/%'` plutôt qu'une requête récursive à chaque
    // contrôle d'accès. Calculé après le premier save (dépend de l'id
    // auto-généré) — voir note de classe.
    @Size(max = 500)
    @Column(name = "chemin", length = 500)
    private String chemin;

    // Chef : autorité générale du sous-arbre. UNIQUE : un employé ne dirige
    // qu'un seul département. Nullable : voir note de dépendance circulaire.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_chef",
            unique = true,
            foreignKey = @ForeignKey(name = "FK_departement_chef")
    )
    private Employe chef;

    // Département parent dans la hiérarchie. Null = département racine.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_departement_parent",
            foreignKey = @ForeignKey(name = "FK_departement_parent")
    )
    private Departement departementParent;

    // Sous-départements directs (0, 1 ou plusieurs), côté inverse de departementParent
    @JsonIgnore
    @OneToMany(mappedBy = "departementParent")
    @Builder.Default
    private Set<Departement> sousDepartements = new HashSet<>();

    // Employés rattachés directement à ce département
    @JsonIgnore
    @OneToMany(mappedBy = "departement")
    @Builder.Default
    private Set<Employe> employes = new HashSet<>();

    public boolean estRacine() {
        return departementParent == null;
    }

    public boolean estFeuille() {
        return sousDepartements == null || sousDepartements.isEmpty();
    }
}