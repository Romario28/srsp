package com.entreprise.gestion.entite;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;

/**
 * Représente la fiche RH d'un employé, indépendante du compte système.
 * Un employé peut exister sans compte (Utilisateur) mais un Utilisateur
 * ne peut être lié qu'à UN SEUL employé (contrainte UNIQUE sur id_employe).
 */
@Entity
@Table(name = "employe", schema = "gestion_utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Employe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_employe")
    private Long id;


    @NotNull
    @Size(max = 20)
    @Column(name = "matricule", nullable = false, unique = true, length = 20)
    private String matricule;


    @NotNull
    @Size(max = 100)
    @Column(name = "nom", nullable = false, length = 100)
    private String nom;

    @NotNull
    @Size(max = 100)
    @Column(name = "prenom", nullable = false, length = 100)
    private String prenom;

    @Size(max = 100)
    @Column(name = "poste", length = 100)
    private String poste;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_departement", nullable = false,
            foreignKey = @ForeignKey(name = "FK_employe_departement"))
    private Departement departement;

    @Column(name = "date_embauche")
    private LocalDate dateEmbauche;

    /**
     * Relation inverse (mappedBy) : Utilisateur porte la FK id_employe.
     * JsonIgnore pour éviter la boucle de sérialisation JSON.
     */
    @JsonIgnore
    @OneToOne(mappedBy = "employe", fetch = FetchType.LAZY)
    private Utilisateur utilisateur;
}
