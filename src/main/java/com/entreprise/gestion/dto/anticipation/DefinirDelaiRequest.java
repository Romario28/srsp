// dto/anticipation/DefinirDelaiRequest.java
package com.entreprise.gestion.dto.anticipation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Surcharge de la fenêtre d'anticipation d'un type.
 * Un champ absent ({@code null}) conserve la valeur effective courante :
 * la prévenance est le seul champ obligatoire, le retard et l'état actif sont optionnels.
 */
@Data
public class DefinirDelaiRequest {

    /** Borne « plus tôt » : jours avant l'échéance à partir desquels on prévient (ex. 120 = 4 mois). */
    @NotNull(message = "La prévenance est obligatoire.")
    @Min(value = 0, message = "La prévenance doit être positive ou nulle.")
    private Integer delaiPrevenanceJours;

    /**
     * Borne « plus tard » : jours de retard tolérés (ex. 30 = 1 mois).
     * 0 = ne jamais afficher les échéances dépassées.
     */
    @Min(value = 0, message = "Le retard doit être positif ou nul.")
    private Integer retardJours;

    /** {@code false} = neutraliser la surcharge sans la supprimer ; absent = (ré)activer. */
    private Boolean actif;
}
