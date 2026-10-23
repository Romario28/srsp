package com.entreprise.gestion.entite.anticipation;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "dossier", schema = "anticipation",
        uniqueConstraints = @UniqueConstraint(name = "UQ_dossier_agent_type_echeance",
                columnNames = {"matricule", "type_dossier", "date_echeance"}),
        indexes = {@Index(name = "idx_dossier_statut", columnList = "statut, date_preparation"),
                   @Index(name = "idx_dossier_matricule", columnList = "matricule")})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Dossier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricule", nullable = false, foreignKey = @ForeignKey(name = "FK_dossier_agent"))
    private Agent agent;

    @Enumerated(EnumType.STRING) @Column(name = "type_dossier", length = 30, nullable = false)
    private TypeAnticipation type;

    @Column(name = "date_preparation", nullable = false) private LocalDate datePreparation;
    @Column(name = "date_echeance", nullable = false)    private LocalDate dateEcheance;

    /** Jour où un gestionnaire a démarré le dossier ; null tant qu'il n'a jamais été démarré. Conservée si le dossier passe sans suite. */
    @Column(name = "date_demarrage") private LocalDate dateDemarrage;

    @Enumerated(EnumType.STRING) @Column(name = "statut", length = 30, nullable = false)
    @Builder.Default private StatutDossier statut = StatutDossier.EN_ATTENTE_DE_TRAITEMENT;

    @Column(name = "motif_sans_suite", length = 500) private String motifSansSuite;

    @Version @Column(name = "version", nullable = false) private Long version;
}
