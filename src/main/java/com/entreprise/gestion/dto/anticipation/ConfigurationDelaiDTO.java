// dto/anticipation/ConfigurationDelaiDTO.java
package com.entreprise.gestion.dto.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;

/**
 * Fenêtre d'anticipation d'un type, telle que vue par l'API de configuration.
 *
 * @param delaiPrevenanceJours      borne « plus tôt » EFFECTIVE (surcharge active, sinon défaut)
 * @param retardJours               borne « plus tard » EFFECTIVE (surcharge active, sinon défaut)
 * @param delaiPrevenanceParDefaut  prévenance par défaut codée en dur, pour comparaison
 * @param retardParDefaut           retard par défaut codé en dur, pour comparaison
 * @param personnalise              true si une surcharge ACTIVE est appliquée en base
 * @param actifEnBase               état du drapeau {@code actif} de la ligne en base ;
 *                                  {@code null} si aucune surcharge n'est enregistrée
 */
public record ConfigurationDelaiDTO(
        TypeAnticipation type,
        int delaiPrevenanceJours,
        int retardJours,
        int delaiPrevenanceParDefaut,
        int retardParDefaut,
        boolean personnalise,
        Boolean actifEnBase
) {}
