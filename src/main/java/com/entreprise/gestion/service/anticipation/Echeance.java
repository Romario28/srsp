// service/anticipation/Echeance.java
package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import java.time.LocalDate;

public record Echeance(
        String matricule, String nomComplet, TypeAnticipation type,
        LocalDate dateEcheance,   // null pour ANOMALIE
        String details
) {}