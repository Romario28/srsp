-- 1. Contraintes qui listent les types (H2)
SELECT tc.TABLE_SCHEMA, tc.TABLE_NAME, tc.CONSTRAINT_NAME, cc.CHECK_CLAUSE
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
WHERE tc.CONSTRAINT_TYPE = 'CHECK'
  AND tc.TABLE_SCHEMA IN ('ANTICIPATION', 'REFERENTIEL_RH')
  AND tc.TABLE_NAME IN ('ALERTE', 'DOSSIER', 'CONFIGURATION_DELAI')
  AND cc.CHECK_CLAUSE LIKE '%TITULARISATION%';

-- 2. Pour chaque ligne trouvée : supprimer la contrainte (nom lu ci-dessus) puis la recréer avec la nouvelle valeur
-- ALTER TABLE anticipation.alerte DROP CONSTRAINT <NOM>;
-- ALTER TABLE anticipation.alerte ADD CONSTRAINT ck_alerte_type
--   CHECK (type_anticipation IN ('DEPART_RETRAITE','AVANCEMENT','AVANCEMENT_CLASSE','TITULARISATION','FIN_CONTRAT','INTEGRATION','ANOMALIE'));
-- ALTER TABLE anticipation.dossier DROP CONSTRAINT <NOM>;
-- ALTER TABLE anticipation.dossier ADD CONSTRAINT ck_dossier_type
--   CHECK (type_dossier IN ('DEPART_RETRAITE','AVANCEMENT','AVANCEMENT_CLASSE','TITULARISATION','FIN_CONTRAT','INTEGRATION','ANOMALIE'));
-- ALTER TABLE referentiel_rh.configuration_delai DROP CONSTRAINT <NOM>;
-- ALTER TABLE referentiel_rh.configuration_delai ADD CONSTRAINT ck_config_delai_type
--   CHECK (type_anticipation IN ('DEPART_RETRAITE','AVANCEMENT','AVANCEMENT_CLASSE','TITULARISATION','FIN_CONTRAT','INTEGRATION','ANOMALIE'));

-- 3. Les dossiers et alertes déjà créés sous « AVANCEMENT » sont requalifiés par le batch (aucune donnée à migrer ici).
--    Contrôle après le premier passage : doit renvoyer 0 ligne (aucun doublon d'échéance)
SELECT matricule, date_echeance, COUNT(*) AS nb
FROM anticipation.dossier
WHERE type_dossier IN ('AVANCEMENT', 'AVANCEMENT_CLASSE')
GROUP BY matricule, date_echeance HAVING COUNT(*) > 1;
