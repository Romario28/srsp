package com.entreprise.gestion.dto.anticipation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Saisie d'une fenêtre d'anticipation : au moins une des deux bornes doit être fournie.
 * Une borne omise conserve la valeur déjà paramétrée (ou le défaut du type).
 */
@Data
public class DefinirDelaiRequest {

    /** Borne haute : nb de jours AVANT l'échéance à partir duquel prévenir. Omise = inchangée. */
    @Min(0)
    private Integer delaiPrevenanceJours;

    /** Borne basse : nb de jours de retard toléré. 0 = ne pas afficher les retards. Omise = inchangée. */
    @Min(0)
    private Integer retardJours;

    /** Surcharge active ou non ; omis = activée. */
    private Boolean actif;

    @AssertTrue(message = "Renseignez au moins une borne : delaiPrevenanceJours ou retardJours.")
    @JsonIgnore
    public boolean isAuMoinsUneBorne() {
        return delaiPrevenanceJours != null || retardJours != null;
    }
}
