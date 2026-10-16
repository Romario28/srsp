// service/anticipation/Echeance.java
package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.entite.anticipation.TypeAnomalie;
import java.time.LocalDate;

public record Echeance(
        String matricule, String nomComplet, TypeAnticipation type,
        LocalDate dateEcheance,   // null pour ANOMALIE
        String details,
        TypeAnomalie typeAnomalie
) {
    public Echeance(String matricule, String nomComplet, TypeAnticipation type, LocalDate dateEcheance, String details) {
        this(matricule, nomComplet, type, dateEcheance, details, null);
    }
}
