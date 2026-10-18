package com.entreprise.gestion.entite.anticipation;

import java.time.LocalDate;
import java.util.function.BiConsumer;
import java.util.function.Function;

/** L'ordre des étapes forme la chaîne chronologique de validation du dossier. */
public enum EtapeDossier {
    CHEF_AVANT_FOP("Chef avant FOP", Dossier::getDateChefAvantFop, Dossier::setDateChefAvantFop),
    FOP("FOP", Dossier::getDateFop, Dossier::setDateFop),
    VISA("VISA", Dossier::getDateVisa, Dossier::setDateVisa),
    CF("CF", Dossier::getDateCf, Dossier::setDateCf),
    PREFET("Préfet", Dossier::getDatePrefet, Dossier::setDatePrefet),
    PERS_TANA("Pers Tana", Dossier::getDatePersTana, Dossier::setDatePersTana),
    SOLDE("Solde", Dossier::getDateSolde, Dossier::setDateSolde),
    FIN("FIN", Dossier::getDateFin, Dossier::setDateFin);

    private final String libelle;
    private final Function<Dossier, LocalDate> lecture;
    private final BiConsumer<Dossier, LocalDate> ecriture;
    EtapeDossier(String libelle, Function<Dossier, LocalDate> lecture, BiConsumer<Dossier, LocalDate> ecriture) {
        this.libelle=libelle; this.lecture=lecture; this.ecriture=ecriture;
    }
    public String libelle() { return libelle; }
    public LocalDate de(Dossier d) { return lecture.apply(d); }
    public void definir(Dossier d, LocalDate date) { ecriture.accept(d,date); }
}
