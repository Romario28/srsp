package com.entreprise.gestion.exception;

import lombok.Getter;

/**
 * Exception métier custom — permet d'attacher un code d'erreur stable
 * (utilisable côté frontend pour des messages traduits) en plus du message.
 *
 * Exemple :
 *   throw new BusinessException("EMPLOYE_DEJA_LIE",
 *       "Cet employé possède déjà un compte utilisateur.");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
