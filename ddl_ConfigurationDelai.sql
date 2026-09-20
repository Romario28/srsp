-- ============================================================================
-- anticipation.configuration_delai — fenêtre d'anticipation à deux bornes
--
-- La table ne contient QUE des surcharges explicites : une ligne absente (ou
-- avec actif = false) fait retomber le type sur son couple par défaut codé en
-- dur dans FenetresParDefaut (service/anticipation/FenetresParDefaut.java).
--
-- Le schéma est créé/mis à jour par Hibernate (spring.jpa.hibernate.ddl-auto),
-- ce script sert de référence pour un déploiement administré ou une future
-- migration Flyway/Liquibase.
-- ============================================================================

-- État attendu après création (H2 / PostgreSQL compatible)
CREATE TABLE IF NOT EXISTS anticipation.configuration_delai
(
    type_anticipation      VARCHAR(30) NOT NULL,  -- DEPART_RETRAITE | AVANCEMENT | TITULARISATION | FIN_CONTRAT
    delai_prevenance_jours INTEGER     NOT NULL,  -- borne « plus tôt » : jours avant l'échéance
    retard_jours           INTEGER     NULL,      -- borne « plus tard » : jours de retard tolérés (0 = jamais)
    actif                  BOOLEAN     NOT NULL,  -- une surcharge inactive est ignorée
    CONSTRAINT pk_configuration_delai PRIMARY KEY (type_anticipation)
);

-- Migration d'une base existante (créée avant l'ajout de la borne de retard).
-- La colonne est nullable : les lignes existantes conservent le retard par
-- défaut du type tant que l'API de configuration n'a pas été rappelée.
ALTER TABLE anticipation.configuration_delai
    ADD COLUMN IF NOT EXISTS retard_jours INTEGER NULL;

-- Exemples de surcharge (prévenance / retard) :
--   UPDATE ... SET delai_prevenance_jours = 120, retard_jours = 30  WHERE type_anticipation = 'AVANCEMENT';
--   UPDATE ... SET actif = FALSE                                   WHERE type_anticipation = 'FIN_CONTRAT';
-- Réinitialisation : DELETE FROM anticipation.configuration_delai WHERE type_anticipation = 'AVANCEMENT';
