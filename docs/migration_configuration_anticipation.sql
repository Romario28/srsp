-- Migration : fenêtre d'anticipation à deux bornes (prévenance / retard)
-- Table : anticipation.configuration_delai
--
-- À exécuter UNE FOIS sur une base existante avant de démarrer la nouvelle version.
-- En développement (ddl-auto: create-drop) comme pour toute base recréée, la colonne
-- est générée automatiquement : cette migration ne concerne que les bases déjà en service
-- (profil prod, H2 fichier + AUTO_SERVER).
--
-- Sans cette migration, le démarrage peut échouer sur l'ALTER TABLE automatique de
-- Hibernate (ajout d'une colonne) ; la ligne existante reste sinon lisible : une colonne
-- retard_jours absente ou NULL fait retomber le type sur son retard par défaut.

ALTER TABLE anticipation.configuration_delai
    ADD COLUMN IF NOT EXISTS retard_jours INTEGER;

-- Contrôle : bornes effectives en base (NULL = valeur par défaut du type)
-- SELECT type_anticipation, delai_prevenance_jours, retard_jours, actif
--   FROM anticipation.configuration_delai;

-- Optionnel : matérialiser explicitement le retard par défaut des surcharges déjà posées
-- (utile si l'on veut ensuite tracer chaque changement de borne sans repasser par le défaut).
-- UPDATE anticipation.configuration_delai SET retard_jours = 365
--  WHERE type_anticipation IN ('DEPART_RETRAITE', 'AVANCEMENT', 'TITULARISATION')
--    AND retard_jours IS NULL;
-- UPDATE anticipation.configuration_delai SET retard_jours = 30
--  WHERE type_anticipation = 'FIN_CONTRAT' AND retard_jours IS NULL;

-- Exemples d'exploitation directe :
-- Prévenir 4 mois avant, ne plus afficher les retards :
-- UPDATE anticipation.configuration_delai SET delai_prevenance_jours = 120, retard_jours = 0
--  WHERE type_anticipation = 'DEPART_RETRAITE';
-- Désactiver une surcharge (retour au défaut codé en dur) :
-- UPDATE anticipation.configuration_delai SET actif = FALSE
--  WHERE type_anticipation = 'FIN_CONTRAT';
