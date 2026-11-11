package com.entreprise.gestion.entite;

public enum StatutUtilisateur {
    ACTIF,
    SUSPENDU,
    DESACTIVE,
    EN_ATTENTE   // [NOUVEAU] demande de compte non validée : ne peut pas se connecter
}
