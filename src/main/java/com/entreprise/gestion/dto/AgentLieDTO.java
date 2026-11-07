package com.entreprise.gestion.dto;

import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.Corps;
import com.entreprise.gestion.entite.anticipation.Grade;

/** Agent lié à un compte, en lecture seule. trouve = faux : matricule saisi, agent absent de la base (lien conservé). */
public record AgentLieDTO(String matricule, boolean trouve, String nom, String prenoms,
                          String gradeCode, String gradeLibelle,
                          String corpsCode, String corpsLibelle, String categorieCode) {

    public static AgentLieDTO absent(String matricule) {
        return new AgentLieDTO(matricule, false, null, null, null, null, null, null, null);
    }

    public static AgentLieDTO de(Agent a) {
        Corps c = a.getCorps();
        Grade g = a.getGrade();
        return new AgentLieDTO(a.getMatricule(), true, a.getNom(), a.getPrenoms(),
                g == null ? null : g.getCode(), g == null ? null : g.getLibelle(),
                c == null ? null : c.getCode(), c == null ? null : c.getLibelle(), c == null ? null : c.getCategorie());
    }
}
