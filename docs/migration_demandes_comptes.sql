-- Migration : autorisation du statut EN_ATTENTE pour les demandes de compte
-- Si une contrainte CHECK existe sur le statut de l'utilisateur, la mettre à jour :
ALTER TABLE gestion_utilisateurs.utilisateur
    DROP CONSTRAINT IF EXISTS utilisateur_statut_check;

ALTER TABLE gestion_utilisateurs.utilisateur
    ADD CONSTRAINT utilisateur_statut_check
    CHECK (statut IN ('ACTIF', 'SUSPENDU', 'DESACTIVE', 'EN_ATTENTE'));
