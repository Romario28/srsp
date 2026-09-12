package com.entreprise.gestion.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Trace immuable d'une action utilisateur (LOGIN, CREATE_EMPLOYE, etc.).
 * Pas de cascade DELETE : les logs doivent survivre à la suppression d'un compte.
 */
@Entity
@Table(
        name = "log_audit",
        schema = "gestion_utilisateurs",
        indexes = {
                @Index(name = "idx_log_audit_date",        columnList = "date_action"),
                @Index(name = "idx_log_audit_utilisateur",  columnList = "id_utilisateur")
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_log")
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "id_utilisateur",
            foreignKey = @ForeignKey(name = "FK_log_audit_utilisateur")
    )
    private Utilisateur utilisateur;

    @NotNull
    @Size(max = 100)
    @Column(name = "action", nullable = false, length = 100)
    private String action;   // LOGIN, LOGOUT, CREATE_EMPLOYE, DELETE_EMPLOYE, CREATE_USER, CHANGE_STATUT…

    @Column(name = "date_action", nullable = false, updatable = false)
    private LocalDateTime dateAction;

    @Size(max = 45)
    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;


    @Column(name = "details", length = 500, columnDefinition = "text")
    private String details;

    @PrePersist
    protected void onCreate() {
        this.dateAction = LocalDateTime.now();
    }
}
