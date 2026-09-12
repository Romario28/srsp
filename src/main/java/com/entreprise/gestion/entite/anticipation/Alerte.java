package com.entreprise.gestion.entite.anticipation;

import com.entreprise.gestion.entite.Utilisateur;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerte", schema = "anticipation",
        uniqueConstraints = @UniqueConstraint(
                name = "UQ_alerte_agent_type_echeance",
                columnNames = {"matricule_agent", "type_anticipation", "date_echeance"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Alerte {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "matricule_agent", length = 6, nullable = false)
    private String matriculeAgent;   // pas de FK stricte vers Agent : l'alerte doit survivre

    @Column(name = "nom_complet_agent", length = 200)
    private String nomCompletAgent;   // dénormalisé volontairement : lisible même si l'agent est radié plus tard

    @Enumerated(EnumType.STRING)
    @Column(name = "type_anticipation", length = 30, nullable = false)
    private TypeAnticipation type;

    @Column(name = "date_echeance")
    private LocalDate dateEcheance;   // null pour une ANOMALIE

    @Column(name = "details", length = 500)
    private String details;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut", length = 20, nullable = false)
    @Builder.Default
    private StatutAlerte statut = StatutAlerte.NOUVELLE;

    @Column(name = "date_detection", nullable = false, updatable = false)
    private LocalDateTime dateDetection;

    @Column(name = "date_derniere_consultation")
    private LocalDateTime dateDerniereConsultation;

    @Column(name = "date_acquittement")
    private LocalDateTime dateAcquittement;

    @ManyToOne @JoinColumn(name = "acquittee_par")
    private Utilisateur acquitteePar;

    @PrePersist
    protected void onCreate() { this.dateDetection = LocalDateTime.now(); }
}