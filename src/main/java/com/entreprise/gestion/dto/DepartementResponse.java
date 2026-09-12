package com.entreprise.gestion.dto;

import com.entreprise.gestion.entite.Departement;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DepartementResponse {
    private Long id;
    private String nomDepartement;
    private String niveau;
    private Long idParent;
    private String nomParent;
    private Long idChef;
    private String nomChef;
    private String chemin;
    private String description;
    private boolean estRacine;
    private boolean estFeuille;

    public static DepartementResponse from(Departement d) {
        var b = DepartementResponse.builder()
                .id(d.getId()).nomDepartement(d.getNomDepartement()).niveau(d.getNiveau() != null ? d.getNiveau() : "")
                .chemin(d.getChemin()).description(d.getDescription())
                .estRacine(d.estRacine()).estFeuille(d.estFeuille());
        if (d.getDepartementParent() != null) {
            b.idParent(d.getDepartementParent().getId());
            b.nomParent(d.getDepartementParent().getNomDepartement());
        }
        if (d.getChef() != null) {
            b.idChef(d.getChef().getId());
            b.nomChef(d.getChef().getPrenom() + " " + d.getChef().getNom());
        }
        return b.build();
    }
}