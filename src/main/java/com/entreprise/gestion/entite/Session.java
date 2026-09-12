package com.entreprise.gestion.entite;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Session JWT active d'un utilisateur.
 * Permet la révocation de token (déconnexion forcée, multi-appareils).
 */
@Entity
@Table(
        name = "session",
        schema = "gestion_utilisateurs",
        indexes = {
                @Index(name = "idx_session_token",      columnList = "token"),
                @Index(name = "idx_session_expiration",  columnList = "date_expiration")
        }
)

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_session")
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "id_utilisateur",
            nullable = false ,
            foreignKey = @ForeignKey(name = "FK_session_utilisateur")
    )
    private Utilisateur utilisateur;

    @NotNull
    @Size(max = 512)
    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;


    @Column(name = "date_creation", nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "adresse_ip", length = 45)
    private String adresseIp;

    @Column(name = "actif", nullable = false)
    @Builder.Default
    private boolean actif = true;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
    }
}
