package com.entreprise.gestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditDTO {
    private Long id;
    private String email;        // null si l'action vient du système
    private String acteur;       // [NOUVEAU] « Prénom Nom (matricule …) » si lié à un agent, sinon l'e-mail
    private String action;
    private LocalDateTime dateAction;
    private String details;
    private String adresseIp;
}
