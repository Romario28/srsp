package com.entreprise.gestion.entite.anticipation;

import com.entreprise.gestion.dto.dossier.DossierDTO;
import com.entreprise.gestion.service.anticipation.Echeance;
import com.entreprise.gestion.service.anticipation.EcheanceAnticipeeDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AgentNomCompletTest {
    private static Agent agent(String prenoms, String nom) {
        return Agent.builder().matricule("123456").prenoms(prenoms).nom(nom).build();
    }

    @Test void prenomEtNom() {
        assertThat(agent("Jean", "Rakoto").nomComplet()).isEqualTo("Jean Rakoto");
    }

    @Test void seulLeNomQuandLePrenomManque() {
        assertThat(agent(null, "Rakoto").nomComplet()).isEqualTo("Rakoto");
        assertThat(agent("", "Rakoto").nomComplet()).isEqualTo("Rakoto");
        assertThat(agent("   ", "Rakoto").nomComplet()).isEqualTo("Rakoto");
    }

    @Test void seulLePrenomQuandLeNomManque() {
        assertThat(agent("Jean", null).nomComplet()).isEqualTo("Jean");
        assertThat(agent("Jean", " ").nomComplet()).isEqualTo("Jean");
    }

    @Test void leMatriculeQuandLesDeuxManquent() {
        assertThat(agent(null, null).nomComplet()).isEqualTo("123456");
        assertThat(agent("", "  ").nomComplet()).isEqualTo("123456");
    }

    @Test void chaineVideSiRienNEstConnu() {
        assertThat(new Agent().nomComplet()).isEmpty();
    }

    @Test void lesEspacesAutourSontRetires() {
        assertThat(agent("  Jean ", " Rakoto  ").nomComplet()).isEqualTo("Jean Rakoto");
    }

    @Test void leMotNullNApparaitJamaisDansLesLignesNiLesDossiers() {
        Agent sansPrenom = agent(null, "Rakoto");
        Echeance e = new Echeance("123456", "x", TypeAnticipation.DEPART_RETRAITE, LocalDate.of(2030, 1, 1), null);
        assertThat(EcheanceAnticipeeDTO.de(sansPrenom, e, null, null).nomComplet()).isEqualTo("Rakoto");

        Dossier d = Dossier.builder().id(1L).agent(agent("Jean", null)).type(TypeAnticipation.AVANCEMENT)
                .datePreparation(LocalDate.of(2029, 1, 1)).dateEcheance(LocalDate.of(2030, 1, 1)).build();
        assertThat(DossierDTO.de(d, LocalDate.of(2026, 10, 16), 1).nomComplet()).isEqualTo("Jean");
    }
}
