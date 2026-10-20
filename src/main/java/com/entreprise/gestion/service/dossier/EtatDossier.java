package com.entreprise.gestion.service.dossier;

/** État d'affichage du dossier, dérivé du statut stocké et de l'échéance. */
public enum EtatDossier {
    A_TRAITER("À traiter"), EN_RETARD("En retard"), EN_COURS("En cours"), TRAITE("Traité"), SANS_SUITE("Sans suite");
    private final String libelle;
    EtatDossier(String libelle){this.libelle=libelle;}
    public String libelle(){return libelle;}
}
