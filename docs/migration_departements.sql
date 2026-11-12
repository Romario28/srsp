-- 0. Contrôle : l'ancienne table departement (hiérarchie) doit avoir disparu (étape 5 de migration_utilisateur_agent.sql).
--    Cette requête doit renvoyer 0 ligne ; sinon, exécutez d'abord cette migration.
SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
 WHERE TABLE_SCHEMA = 'GESTION_UTILISATEURS' AND TABLE_NAME = 'DEPARTEMENT' AND COLUMN_NAME NOT IN ('ID_DEPARTEMENT', 'LIBELLE');

-- 1. Table des départements (information seulement)
CREATE TABLE IF NOT EXISTS gestion_utilisateurs.departement (
    id_departement BIGINT AUTO_INCREMENT NOT NULL,
    libelle        VARCHAR(200) NOT NULL,
    CONSTRAINT pk_departement PRIMARY KEY (id_departement),
    CONSTRAINT UQ_departement_libelle UNIQUE (libelle)
);

-- 2. Département de chaque compte (nul pour les comptes existants)
ALTER TABLE gestion_utilisateurs.utilisateur ADD COLUMN IF NOT EXISTS id_departement BIGINT;
-- Si l'application a déjà créé cette contrainte au démarrage, cette ligne échoue : ignorez l'erreur.
ALTER TABLE gestion_utilisateurs.utilisateur ADD CONSTRAINT FK_utilisateur_departement
    FOREIGN KEY (id_departement) REFERENCES gestion_utilisateurs.departement (id_departement);
