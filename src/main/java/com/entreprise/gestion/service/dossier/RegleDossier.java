package com.entreprise.gestion.service.dossier;

import com.entreprise.gestion.entite.anticipation.Dossier;
import com.entreprise.gestion.entite.anticipation.StatutDossier;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public final class RegleDossier {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** État de présentation : le retard ne concerne que les dossiers non démarrés dont l'échéance est strictement dépassée. */
    public static EtatDossier etat(StatutDossier statut, LocalDate dateEcheance, LocalDate aujourdhui) {
        return switch (statut) {
            case SANS_SUITE -> EtatDossier.SANS_SUITE;
            case EN_COURS -> EtatDossier.EN_COURS;
            case EN_ATTENTE_DE_TRAITEMENT -> dateEcheance.isBefore(aujourdhui) ? EtatDossier.EN_RETARD : EtatDossier.A_TRAITER;
        };
    }

    public static boolean enRetard(Dossier d, LocalDate aujourdhui) {
        return etat(d.getStatut(), d.getDateEcheance(), aujourdhui) == EtatDossier.EN_RETARD;
    }

    /** Une réouverture restitue l'état d'avant l'abandon : « en cours » si le dossier avait été démarré, sinon « à traiter ». */
    public static StatutDossier statutApresReouverture(Dossier d) {
        return d.getDateDemarrage() != null ? StatutDossier.EN_COURS : StatutDossier.EN_ATTENTE_DE_TRAITEMENT;
    }

    public static String fmt(LocalDate d) { return d == null ? "—" : d.format(FMT); }

    private RegleDossier() {}
}
