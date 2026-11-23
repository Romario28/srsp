package com.entreprise.gestion.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemandesComptesDiffusionTest {
    private final DemandesComptesDiffusion diffusion = new DemandesComptesDiffusion();

    @Test void unCompteNeGardeQueSesCinqDerniersFlux() {
        for (int i = 0; i < 7; i++) diffusion.abonner(1L);
        assertThat(diffusion.nombreAbonnes()).isEqualTo(5);
        diffusion.abonner(2L);
        assertThat(diffusion.nombreAbonnes()).isEqualTo(6);   // le plafond est par compte
    }

    @Test void notifierEtBattementSupportentLaListeVideOuInactive() {
        diffusion.notifier();
        diffusion.battement();
        diffusion.abonner(1L);
        diffusion.notifier();
        diffusion.battement();
        assertThat(diffusion.nombreAbonnes()).isEqualTo(1);   // hors serveur, rien ne casse l'envoi : il est mis en attente
    }
}
