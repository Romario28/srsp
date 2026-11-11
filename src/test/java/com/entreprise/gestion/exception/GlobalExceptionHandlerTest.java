package com.entreprise.gestion.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test void uneErreurInattendueNeRevelePasSonMessage() {
        ResponseEntity<Map<String, String>> reponse = handler.handleGeneric(
                new IllegalStateException("A different object with the same identifier value — détail-interne"));

        assertThat(reponse.getStatusCode().value()).isEqualTo(500);
        assertThat(reponse.getBody()).containsEntry("code", "ERREUR_INTERNE");
        assertThat(reponse.getBody().get("error"))
                .doesNotContain("détail-interne")
                .contains("référence");
    }

    @Test void chaqueErreurPorteUneReferenceDifferente() {
        String a = handler.handleGeneric(new RuntimeException("x")).getBody().get("error");
        String b = handler.handleGeneric(new RuntimeException("x")).getBody().get("error");
        assertThat(a).isNotEqualTo(b);
    }
}
