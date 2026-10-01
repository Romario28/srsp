package com.entreprise.gestion.config;

import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.entite.anticipation.*;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.repository.anticipation.AlerteRepository;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.repository.referentiel.CorpsRepository;
import com.entreprise.gestion.repository.referentiel.GradeRepository;
import com.entreprise.gestion.repository.referentiel.IndiceGrdCorpsRepository;
import com.entreprise.gestion.repository.referentiel.SanctionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Component
@Profile({"local", "dev"})
@Order(2) // après DataInitializer (utilisateurs) pour pouvoir lier une alerte acquittée
@RequiredArgsConstructor
public class AnticipationDevDataInitializer implements CommandLineRunner {

    private final GradeRepository gradeRepository;
    private final CorpsRepository corpsRepository;
    private final IndiceGrdCorpsRepository indiceGrdCorpsRepository;
    private final SanctionRepository sanctionRepository;
    private final AgentRepository agentRepository;
    private final AlerteRepository alerteRepository;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (agentRepository.count() > 0) {
            return;
        }

        Grade stagiaire = gradeRepository.save(Grade.builder().code("ST0E").libelle("Stagiaire").build());
        Grade grade1A   = gradeRepository.save(Grade.builder().code("1A").libelle("1ère classe A").build());
        Grade grade2A   = gradeRepository.save(Grade.builder().code("2A").libelle("2ème classe A").build());

        Corps corpsAdm = corpsRepository.save(Corps.builder()
                .code("ADM").categorie("I").libelle("Administrateur").build());
        Corps corpsTec = corpsRepository.save(Corps.builder()
                .code("TEC").categorie("II").libelle("Technicien").build());

        indiceGrdCorpsRepository.save(IndiceGrdCorps.builder()
                .grade(stagiaire).corps(corpsAdm).indice("225").dureeRequise(2).build());
        indiceGrdCorpsRepository.save(IndiceGrdCorps.builder()
                .grade(grade1A).corps(corpsAdm).indice("350").dureeRequise(2).build());
        indiceGrdCorpsRepository.save(IndiceGrdCorps.builder()
                .grade(grade2A).corps(corpsTec).indice("400").dureeRequise(3).build());

        Sanction enActivite = sanctionRepository.save(Sanction.builder().code("00").libelle("En activité").build());
        Sanction horsActivite = sanctionRepository.save(Sanction.builder().code("21").libelle("Hors activité (exemple)").build());

        Grade gradeMj = gradeRepository.save(Grade.builder().code("MJ00").libelle("Palier ELD (exemple)").build());
        Corps corpsEld = corpsRepository.save(Corps.builder().code("4100").categorie("01").libelle("Corps ELD (exemple)").build());
        indiceGrdCorpsRepository.save(IndiceGrdCorps.builder()
                .grade(gradeMj).corps(corpsEld).indice("300").dureeRequise(2).build());

        LocalDate aujourdhui = LocalDate.now();

        // Retraite dans ~90 jours (âge légal 60)
        Agent retraiteProche = agentRepository.save(Agent.builder()
                .matricule("A00001").nom("Rakoto").prenoms("Jean")
                .dateNaissance(aujourdhui.minusYears(60).plusDays(90))
                .statut(StatutAgent.FONCTIONNAIRE)
                .grade(grade1A).corps(corpsAdm)
                .avanceDate(aujourdhui.minusYears(5))
                .dateDebutContrat(aujourdhui.minusYears(20))
                .build());

        // Avancement dû dans ~30 jours
        Agent avancementDu = agentRepository.save(Agent.builder()
                .matricule("A00002").nom("Rabe").prenoms("Marie")
                .dateNaissance(aujourdhui.minusYears(35))
                .statut(StatutAgent.FONCTIONNAIRE)
                .grade(grade1A).corps(corpsAdm)
                .avanceDate(aujourdhui.minusYears(2).plusDays(30))
                .dateDebutContrat(aujourdhui.minusYears(10))
                .build());

        // Titularisation (grade stagiaire) dans ~45 jours
        Agent titularisation = agentRepository.save(Agent.builder()
                .matricule("A00003").nom("Andria").prenoms("Paul")
                .dateNaissance(aujourdhui.minusYears(28))
                .statut(StatutAgent.FONCTIONNAIRE)
                .grade(stagiaire).corps(corpsAdm)
                .avanceDate(null)
                .dateDebutContrat(aujourdhui.minusYears(2).plusDays(45))
                .build());

        // Fin de contrat dans ~40 jours
        Agent finContrat = agentRepository.save(Agent.builder()
                .matricule("A00004").nom("Rasoa").prenoms("Lala")
                .dateNaissance(aujourdhui.minusYears(40))
                .statut(StatutAgent.CONTRACTUEL)
                .grade(grade2A).corps(corpsTec)
                .avanceDate(aujourdhui.minusYears(1))
                .dateDebutContrat(aujourdhui.minusYears(1))
                .dateFinContrat(aujourdhui.plusDays(40))
                .build());

        // Fin de contrat déjà dépassée (retard ~10 j, dans la fenêtre)
        Agent finContratRetard = agentRepository.save(Agent.builder()
                .matricule("A00005").nom("Ravelo").prenoms("Hery")
                .dateNaissance(aujourdhui.minusYears(45))
                .statut(StatutAgent.CONTRACTUEL)
                .grade(grade2A).corps(corpsTec)
                .dateDebutContrat(aujourdhui.minusYears(2))
                .dateFinContrat(aujourdhui.minusDays(10))
                .build());

        // Anomalie : grade et corps manquants
        Agent anomalie = agentRepository.save(Agent.builder()
                .matricule("A00006").nom("Razafy").prenoms("Nina")
                .dateNaissance(aujourdhui.minusYears(30))
                .statut(StatutAgent.FONCTIONNAIRE)
                .dateDebutContrat(aujourdhui.minusYears(5))
                .build());

        // Hors fenêtre : retraite dans 3 ans → invisible avec défauts
        agentRepository.save(Agent.builder()
                .matricule("A00007").nom("Far").prenoms("Loin")
                .dateNaissance(aujourdhui.minusYears(57))
                .statut(StatutAgent.FONCTIONNAIRE)
                .grade(grade1A).corps(corpsAdm)
                .avanceDate(aujourdhui.minusYears(1))
                .dateDebutContrat(aujourdhui.minusYears(15))
                .build());

        // Couverture des statuts, anomalies et filtre « en activité ».
        agentRepository.save(Agent.builder()
                .matricule("A00008").nom("Ranaivo").prenoms("Toky")
                .dateNaissance(aujourdhui.minusYears(60).plusDays(400))
                .statut(StatutAgent.ELD).grade(gradeMj).corps(corpsEld).sanction(enActivite)
                .avanceDate(aujourdhui.minusYears(2).plusDays(20))
                .dateDebutContrat(aujourdhui.minusYears(6)).dateFinContrat(aujourdhui.plusDays(70)).build());

        agentRepository.save(Agent.builder()
                .matricule("A00009").nom("Rabary").prenoms("Sitraka")
                .dateNaissance(aujourdhui.minusYears(60).plusDays(200))
                .grade(grade1A).corps(corpsAdm)
                .avanceDate(aujourdhui.minusYears(2).plusDays(60))
                .dateDebutContrat(aujourdhui.minusYears(12)).dateFinContrat(aujourdhui.minusDays(20)).build());

        agentRepository.save(Agent.builder()
                .matricule("A00010").nom("Rabemanana").prenoms("Lanto")
                .dateNaissance(aujourdhui.minusYears(39)).statut(StatutAgent.FONCTIONNAIRE)
                .grade(grade2A).corps(corpsAdm).avanceDate(aujourdhui.minusYears(1))
                .dateDebutContrat(aujourdhui.minusYears(9)).build());

        agentRepository.save(Agent.builder()
                .matricule("A00011").nom("Randrianasolo").prenoms("Faly")
                .statut(StatutAgent.CONTRACTUEL).grade(grade1A).corps(corpsAdm)
                .avanceDate(aujourdhui.minusYears(1)).dateDebutContrat(aujourdhui.minusYears(3)).build());

        agentRepository.save(Agent.builder()
                .matricule("A00012").nom("Andrianjaka").prenoms("Ravo")
                .dateNaissance(aujourdhui.minusYears(60).plusDays(30)).statut(StatutAgent.FONCTIONNAIRE)
                .grade(grade1A).corps(corpsAdm).sanction(horsActivite)
                .avanceDate(aujourdhui.minusYears(2).plusDays(10))
                .dateDebutContrat(aujourdhui.minusYears(25)).build());

        Utilisateur admin = utilisateurRepository.findByEmail("admin@entreprise.mg").orElse(null);

        alerteRepository.save(Alerte.builder()
                .matriculeAgent(retraiteProche.getMatricule())
                .nomCompletAgent("Jean Rakoto")
                .type(TypeAnticipation.DEPART_RETRAITE)
                .dateEcheance(retraiteProche.getDateNaissance().plusYears(60))
                .details("Départ à la retraite")
                .statut(StatutAlerte.NOUVELLE)
                .build());

        alerteRepository.save(Alerte.builder()
                .matriculeAgent(avancementDu.getMatricule())
                .nomCompletAgent("Marie Rabe")
                .type(TypeAnticipation.AVANCEMENT)
                .dateEcheance(avancementDu.getAvanceDate().plusYears(2))
                .statut(StatutAlerte.VUE)
                .dateDerniereConsultation(LocalDateTime.now().minusDays(1))
                .build());

        alerteRepository.save(Alerte.builder()
                .matriculeAgent(titularisation.getMatricule())
                .nomCompletAgent("Paul Andria")
                .type(TypeAnticipation.TITULARISATION)
                .dateEcheance(titularisation.getDateDebutContrat().plusYears(2))
                .details("Ancrage : date de début de contrat (avance_date absente)")
                .statut(StatutAlerte.NOUVELLE)
                .build());

        alerteRepository.save(Alerte.builder()
                .matriculeAgent(finContrat.getMatricule())
                .nomCompletAgent("Lala Rasoa")
                .type(TypeAnticipation.FIN_CONTRAT)
                .dateEcheance(finContrat.getDateFinContrat())
                .statut(StatutAlerte.NOUVELLE)
                .build());

        alerteRepository.save(Alerte.builder()
                .matriculeAgent(finContratRetard.getMatricule())
                .nomCompletAgent("Hery Ravelo")
                .type(TypeAnticipation.FIN_CONTRAT)
                .dateEcheance(finContratRetard.getDateFinContrat())
                .statut(StatutAlerte.NOUVELLE)
                .build());

        alerteRepository.save(Alerte.builder()
                .matriculeAgent(anomalie.getMatricule())
                .nomCompletAgent("Nina Razafy")
                .type(TypeAnticipation.ANOMALIE)
                .dateEcheance(null)
                .details("Grade et corps manquants")
                .statut(StatutAlerte.NOUVELLE)
                .build());

        alerteRepository.save(Alerte.builder().matriculeAgent("A00008").nomCompletAgent("Toky Ranaivo")
                .type(TypeAnticipation.AVANCEMENT).dateEcheance(aujourdhui.plusDays(20))
                .statut(StatutAlerte.NOUVELLE).build());
        alerteRepository.save(Alerte.builder().matriculeAgent("A00009").nomCompletAgent("Sitraka Rabary")
                .type(TypeAnticipation.FIN_CONTRAT).dateEcheance(aujourdhui.minusDays(20))
                .statut(StatutAlerte.NOUVELLE).build());
        alerteRepository.save(Alerte.builder().matriculeAgent("A00010").nomCompletAgent("Lanto Rabemanana")
                .type(TypeAnticipation.ANOMALIE).dateEcheance(null)
                .details("Durée requise non renseignée pour ADM/I/2A")
                .statut(StatutAlerte.NOUVELLE).build());
        alerteRepository.save(Alerte.builder().matriculeAgent("A00011").nomCompletAgent("Faly Randrianasolo")
                .type(TypeAnticipation.ANOMALIE).dateEcheance(null)
                .details("Date de naissance manquante")
                .statut(StatutAlerte.NOUVELLE).build());

        for (int i = 1; i <= 25; i++) {
            alerteRepository.save(Alerte.builder()
                    .matriculeAgent(String.format("T%05d", i))
                    .nomCompletAgent("Agent test " + i)
                    .type(TypeAnticipation.FIN_CONTRAT)
                    .dateEcheance(aujourdhui.plusDays(i))
                    .statut(StatutAlerte.NOUVELLE).build());
        }

        if (admin != null) {
            alerteRepository.save(Alerte.builder()
                    .matriculeAgent(retraiteProche.getMatricule())
                    .nomCompletAgent("Jean Rakoto")
                    .type(TypeAnticipation.DEPART_RETRAITE)
                    .dateEcheance(aujourdhui.minusYears(1)) // autre échéance, déjà acquittée
                    .details("Ancienne échéance recalculée")
                    .statut(StatutAlerte.ACQUITTEE)
                    .dateAcquittement(LocalDateTime.now().minusDays(7))
                    .acquitteePar(admin)
                    .build());
        }

        System.out.println("""

            ╔═══════════════════════════════════════════════════════════════╗
            ║   Anticipation DEV : 12 agents + référentiels + 36 alertes    ║
            ║   Profils actifs : local / dev                                 ║
            ╚═══════════════════════════════════════════════════════════════╝
            """);
    }
}
