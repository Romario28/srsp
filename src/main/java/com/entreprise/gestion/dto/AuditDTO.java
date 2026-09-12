package com.entreprise.gestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditDTO {
    private Long id;
    private String email;
    private String action;
    private LocalDateTime dateAction;
    private String details;
    private String adresseIp;
}
