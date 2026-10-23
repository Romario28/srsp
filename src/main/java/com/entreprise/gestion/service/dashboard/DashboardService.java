package com.entreprise.gestion.service.dashboard;

import com.entreprise.gestion.dto.dashboard.EcheancesAVenirDTO;
import com.entreprise.gestion.dto.dashboard.EffectifsDTO;
import com.entreprise.gestion.dto.dashboard.EffectifsDTO.*;
import com.entreprise.gestion.entite.anticipation.Agent;
import com.entreprise.gestion.entite.anticipation.CategorieCalendrier;
import com.entreprise.gestion.entite.anticipation.StatutAgent;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.referentiel.AgentRepository;
import com.entreprise.gestion.service.anticipation.ConfigurationDelaiService;
import com.entreprise.gestion.service.anticipation.Echeance;
import com.entreprise.gestion.service.anticipation.FenetreAnticipation;
import com.entreprise.gestion.service.anticipation.FiltreAgents;
import com.entreprise.gestion.service.anticipation.MoteurAnticipation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class DashboardService {
    static final int AGE_MIN = 25, AGE_PAS = 5, AGE_MAX = 65;
    static final int NB_MOIS = 12, NB_ANNEES = 5, JOURS_MAX = 366;

    private final AgentRepository agentRepository;
    private final MoteurAnticipation moteur;
    private final ConfigurationDelaiService configurationDelaiService;

    private record CleAge(int tranche, String sexe, String categorie) {}

    @Transactional(readOnly = true)
    public EffectifsDTO effectifs(FiltreAgents filtre) { return effectifs(LocalDate.now(), filtre); }

    EffectifsDTO effectifs(LocalDate aujourdhui) { return effectifs(aujourdhui, FiltreAgents.AUCUN); }

    /** Date injectée pour tester sans dépendre de l'horloge. Un parcours, sans calcul des autres échéances. */
    EffectifsDTO effectifs(LocalDate aujourdhui, FiltreAgents filtre) {
        LocalDate debutPeriode = aujourdhui.withDayOfMonth(1);
        long fonctionnaires = 0, contractuels = 0, eld = 0, sansStatut = 0, sansNaissance = 0, dejaAtteint = 0;
        Map<CleAge, Long> ages = new TreeMap<>(Comparator.comparingInt(CleAge::tranche)
                .thenComparing(CleAge::sexe).thenComparing(CleAge::categorie));
        long[] mois = new long[NB_MOIS];
        long[] annees = new long[NB_ANNEES];

        List<Agent> agents = actifsDuPerimetre(filtre);
        for (Agent a : agents) {
            StatutAgent statut = a.getStatut();
            if (statut == StatutAgent.FONCTIONNAIRE) fonctionnaires++;
            else if (statut == StatutAgent.CONTRACTUEL) contractuels++;
            else if (statut == StatutAgent.ELD) eld++;
            else sansStatut++;

            if (a.getDateNaissance() == null) { sansNaissance++; continue; }
            int age = age(a.getDateNaissance(), aujourdhui);
            ages.merge(new CleAge(tranche(age), sexe(a.getSexe()), categorie(a)), 1L, Long::sum);

            LocalDate retraite = moteur.dateRetraite(a);
            if (retraite.isBefore(debutPeriode)) { dejaAtteint++; continue; }
            int moisDepuisDebut = (retraite.getYear() - debutPeriode.getYear()) * 12
                    + retraite.getMonthValue() - debutPeriode.getMonthValue();
            if (moisDepuisDebut < NB_MOIS) mois[moisDepuisDebut]++;
            int anneeDepuisDebut = retraite.getYear() - debutPeriode.getYear();
            if (anneeDepuisDebut < NB_ANNEES) annees[anneeDepuisDebut]++;
        }

        List<LigneAge> lignes = ages.entrySet().stream()
                .map(e -> new LigneAge(e.getKey().tranche(), e.getKey().sexe(), e.getKey().categorie(), e.getValue()))
                .toList();
        List<Periode> parMois = IntStream.range(0, NB_MOIS)
                .mapToObj(i -> new Periode(YearMonth.from(debutPeriode).plusMonths(i).toString(), mois[i])).toList();
        List<Periode> parAnnee = IntStream.range(0, NB_ANNEES)
                .mapToObj(i -> new Periode(String.valueOf(debutPeriode.getYear() + i), annees[i])).toList();

        return new EffectifsDTO(agents.size(), fonctionnaires, contractuels, eld, sansStatut,
                new Ages(new Bornes(AGE_MIN, AGE_PAS, AGE_MAX), moteur.ageRetraite(), lignes, sansNaissance),
                new Retraites(debutPeriode, parMois, parAnnee, dejaAtteint));
    }

    @Transactional(readOnly = true)
    public EcheancesAVenirDTO echeances(int jours, FiltreAgents filtre) { return echeances(jours, LocalDate.now(), filtre); }

    EcheancesAVenirDTO echeances(int jours, LocalDate aujourdhui) { return echeances(jours, aujourdhui, FiltreAgents.AUCUN); }

    /** Les deux bornes sont inclusives : pour 30 jours, la borne finale est aujourd'hui + 29 jours. */
    EcheancesAVenirDTO echeances(int jours, LocalDate aujourdhui, FiltreAgents filtre) {
        if (jours < 1 || jours > JOURS_MAX) {
            throw new BusinessException("JOURS_INVALIDES", "Le nombre de jours doit être compris entre 1 et " + JOURS_MAX + ".");
        }
        LocalDate au = aujourdhui.plusDays(jours - 1L);
        YearMonth moisSuivant = YearMonth.from(aujourdhui).plusMonths(1);
        LocalDate debutMois = moisSuivant.atDay(1), finMois = moisSuivant.atEndOfMonth();
        LocalDate debutAnnee = LocalDate.of(aujourdhui.getYear() + 1, 1, 1);
        LocalDate finAnnee = LocalDate.of(aujourdhui.getYear() + 1, 12, 31);
        Map<TypeAnticipation, FenetreAnticipation> fenetres = configurationDelaiService.resoudreToutes();

        Compteur prochains = new Compteur(), enPreparation = new Compteur();
        Compteur moisProchain = new Compteur(), anneeProchaine = new Compteur();
        for (Agent agent : actifsDuPerimetre(filtre)) {
            for (Echeance echeance : moteur.calculerEcheances(agent)) {
                LocalDate date = echeance.dateEcheance();
                if (echeance.type() == TypeAnticipation.ANOMALIE || date == null) continue;
                CategorieCalendrier categorie = CategorieCalendrier.de(echeance.type(), agent.getStatut());
                if (categorie == null) continue;
                if (dans(date, aujourdhui, au)) prochains.ajouter(categorie);
                FenetreAnticipation fenetre = fenetres.get(echeance.type());
                if (fenetre != null && fenetre.contient(date, aujourdhui)) enPreparation.ajouter(categorie);
                if (dans(date, debutMois, finMois)) moisProchain.ajouter(categorie);
                if (dans(date, debutAnnee, finAnnee)) anneeProchaine.ajouter(categorie);
            }
        }
        return new EcheancesAVenirDTO(jours, aujourdhui, au, prochains.total, prochains.figer(), enPreparation.figer(),
                new EcheancesAVenirDTO.PeriodeEcheances(debutMois, finMois, moisProchain.total, moisProchain.figer()),
                new EcheancesAVenirDTO.PeriodeEcheances(debutAnnee, finAnnee, anneeProchaine.total, anneeProchaine.figer()));
    }

    /** Agents en activité qui passent le périmètre ; les attributs absents ne correspondent pas à un filtre posé. */
    private List<Agent> actifsDuPerimetre(FiltreAgents filtre) {
        FiltreAgents f = filtre == null ? FiltreAgents.AUCUN : filtre;
        return agentRepository.findAllActifs().stream().filter(f::accepte).toList();
    }

    private static boolean dans(LocalDate date, LocalDate debut, LocalDate fin) {
        return !date.isBefore(debut) && !date.isAfter(fin);
    }

    /** Compteurs toujours complets : le JSON expose chaque catégorie même quand son total vaut zéro. */
    private static final class Compteur {
        private final EnumMap<CategorieCalendrier, Long> parCategorie = new EnumMap<>(CategorieCalendrier.class);
        private long total;
        private Compteur() { for (CategorieCalendrier c : CategorieCalendrier.values()) parCategorie.put(c, 0L); }
        private void ajouter(CategorieCalendrier categorie) { parCategorie.merge(categorie, 1L, Long::sum); total++; }
        private Map<CategorieCalendrier, Long> figer() { return Collections.unmodifiableMap(parCategorie); }
    }

    private static int age(LocalDate naissance, LocalDate aujourdhui) {
        int age = aujourdhui.getYear() - naissance.getYear();
        return aujourdhui.isBefore(naissance.plusYears(age)) ? age - 1 : age;
    }

    private static int tranche(int age) {
        if (age < AGE_MIN) return 0;
        if (age >= AGE_MAX) return (AGE_MAX - AGE_MIN) / AGE_PAS + 1;
        return (age - AGE_MIN) / AGE_PAS + 1;
    }

    private static String sexe(String valeur) {
        if (valeur == null || valeur.isBlank()) return "?";
        String normalise = valeur.trim().toUpperCase(Locale.ROOT);
        return normalise.equals("M") || normalise.equals("F") ? normalise : "?";
    }

    private static String categorie(Agent agent) {
        return agent.getCorps() == null || agent.getCorps().getCategorie() == null ? "" : agent.getCorps().getCategorie();
    }
}
