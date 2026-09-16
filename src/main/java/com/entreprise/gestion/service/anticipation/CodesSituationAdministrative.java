// CodesSituationAdministrative.java
package com.entreprise.gestion.service.anticipation;

/**
 * Whitelist des situations administratives considérées comme « en activité ».
 * Un agent est actif si {@code sanction.code = CODE_ACTIF} ou si {@code sanction} est null
 * (absence de sanction = toujours traité comme actif).
 */
public final class CodesSituationAdministrative {

    /** Unique code de situation administrative correspondant à un agent en activité. */
    public static final String CODE_ACTIF = "00";

    private CodesSituationAdministrative() {}
}
