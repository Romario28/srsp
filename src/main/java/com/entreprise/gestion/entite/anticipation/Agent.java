// Agent.java
package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;

import java.time.LocalDate;

@Entity
@Table(name = "agents", schema = "referentiel_rh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Agent {

    @Id
    @Column(name = "matricule", length = 6)
    private String matricule;

    @Column(name = "poste_numero", length = 7)
    private String posteNumero;

    @Column(name = "nom", length = 100 )
    private String nom;

    @Column(name = "prenoms", length = 100)
    private String prenoms;

    @Column(name = "date_naiss")
    private LocalDate dateNaissance;

    @Column(name = "cin", length = 20)
    private String cin;

    @Column(name = "sexe", length = 1)
    private String sexe;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20)
    private StatutAgent statut;


    

 /*   @ManyToOne @JoinColumn(name = "corps_code")
    private Corps corps;

    @ManyToOne @JoinColumn(name = "grade_code")
    private Grade grade;

*/
     @ManyToOne(fetch = FetchType.LAZY)
     @JoinColumns({
             @JoinColumn(name = "corps_code", referencedColumnName = "code"),
             @JoinColumn(name = "categorie_code", referencedColumnName = "categorie")
     })
     private Corps corps;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grade_code", referencedColumnName = "code")
    private Grade grade;

    @Column(name = "indice", length = 20)
    private String indiceActuel;


    @ManyToOne @JoinColumn(name = "hee_code")
    private Hee hee;

    @Column(name = "hee_categorie_code", length = 20)
    private String heeCategorieCode;

    @Column(name = "section_code", length = 20)
    private String sectionCode;

    @ManyToOne @JoinColumn(name = "fiv_code", referencedColumnName = "code_localite")
    private Localite localite;

    @ManyToOne @JoinColumn(name = "sanction_code")
    private Sanction sanction;

    @ManyToOne @JoinColumn(name = "soa")
    private Soa soa;

    @Column(name = "date_debut_contrat")
    private LocalDate dateDebutContrat;

    @Column(name = "date_fin_contrat")
    private LocalDate dateFinContrat;

    @Column(name = "avance_date")
    private LocalDate avanceDate;

    @Column(name = "reg_code", length = 10)
    private String regCode;

    @ManyToOne @JoinColumn(name = "min_code")
    private Ministere ministere;
}
