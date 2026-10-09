package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.StatutAgent;

import java.time.LocalDate;

/** Fiche complète d'un agent, à charger uniquement à l'ouverture de sa fiche. */
public record AgentFicheDTO(
        String matricule, String nom, String prenoms, StatutAgent statut,
        LocalDate dateNaissance, String sexe, String cin, String posteNumero,
        Reference corps, String categorieCode, Reference grade, String indice,
        LocalDate dateDebutContrat, LocalDate dateFinContrat, LocalDate avanceDate,
        Reference situation, Reference hee, String heeCategorieCode, String sectionCode,
        Reference localite, Reference soa, String regCode, Reference ministere
) {
    /** Code et libellé d'un référentiel. */
    public record Reference(String code, String libelle) {}

    public static AgentFicheDTO from(Agent a) {
        return new AgentFicheDTO(
                a.getMatricule(), a.getNom(), a.getPrenoms(), a.getStatut(),
                a.getDateNaissance(), a.getSexe(), a.getCin(), a.getPosteNumero(),
                a.getCorps() != null ? new Reference(a.getCorps().getCode(), a.getCorps().getLibelle()) : null,
                a.getCorps() != null ? a.getCorps().getCategorie() : null,
                a.getGrade() != null ? new Reference(a.getGrade().getCode(), a.getGrade().getLibelle()) : null,
                a.getIndiceActuel(),
                a.getDateDebutContrat(), a.getDateFinContrat(), a.getAvanceDate(),
                a.getSanction() != null ? new Reference(a.getSanction().getCode(), a.getSanction().getLibelle()) : null,
                a.getHee() != null ? new Reference(a.getHee().getCode(), a.getHee().getLibelle()) : null,
                a.getHeeCategorieCode(), a.getSectionCode(),
                a.getLocalite() != null ? new Reference(a.getLocalite().getCode(), a.getLocalite().getNom()) : null,
                a.getSoa() != null ? new Reference(a.getSoa().getCode(), a.getSoa().getLibelle()) : null,
                a.getRegCode(),
                a.getMinistere() != null ? new Reference(a.getMinistere().getCode(), a.getMinistere().getLibelle()) : null);
    }
}
