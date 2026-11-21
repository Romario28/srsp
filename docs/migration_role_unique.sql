-- ATTENTION : sauvegardez la base avant. Suppression définitive des tables permission, role_permission et utilisateur_role
-- (la date d'attribution des rôles est perdue). Sans objet en profil local (H2 en mémoire) : le schéma est recréé.
-- À exécuter AVANT de démarrer la nouvelle version sur une base persistante.

-- 0. Contrôles (information) -------------------------------------------------------------------------------
-- 0a. Comptes avec plus d'un rôle : l'étape 2 garde le plus élevé (ADMIN > CHEF > RH > AGENT > autres).
SELECT u.email, COUNT(*) AS nb_roles
  FROM gestion_utilisateurs.utilisateur u
  JOIN gestion_utilisateurs.utilisateur_role ur ON ur.id_utilisateur = u.id_utilisateur
 GROUP BY u.email HAVING COUNT(*) > 1;

-- 0b. Comptes sans aucun rôle : à corriger AVANT l'étape 3, qui échoue tant qu'il en reste.
SELECT u.email FROM gestion_utilisateurs.utilisateur u
 WHERE NOT EXISTS (SELECT 1 FROM gestion_utilisateurs.utilisateur_role ur WHERE ur.id_utilisateur = u.id_utilisateur);

-- 0c. Rôles hors des quatre profils connus (l'application refuse de les attribuer).
SELECT nom_role FROM gestion_utilisateurs.role
 WHERE nom_role NOT IN ('ROLE_ADMIN', 'ROLE_CHEF', 'ROLE_RH', 'ROLE_AGENT');

-- 1. Nouvelle colonne : rôle unique du compte (nulle le temps de la reprise)
ALTER TABLE gestion_utilisateurs.utilisateur ADD COLUMN IF NOT EXISTS id_role BIGINT;

-- 2. Reprise : pour chaque compte, le rôle le plus élevé de l'ancienne table de jonction
UPDATE gestion_utilisateurs.utilisateur u
   SET id_role = (
        SELECT r.id_role
          FROM gestion_utilisateurs.utilisateur_role ur
          JOIN gestion_utilisateurs.role r ON r.id_role = ur.id_role
         WHERE ur.id_utilisateur = u.id_utilisateur
         ORDER BY CASE r.nom_role WHEN 'ROLE_ADMIN' THEN 1 WHEN 'ROLE_CHEF' THEN 2
                                  WHEN 'ROLE_RH' THEN 3 WHEN 'ROLE_AGENT' THEN 4 ELSE 5 END, r.id_role
         FETCH FIRST 1 ROW ONLY)
 WHERE u.id_role IS NULL;

-- 3. Le rôle devient obligatoire. Échoue volontairement si un compte n'en a pas (voir 0b) :
--    donnez-lui d'abord un rôle, par exemple : UPDATE gestion_utilisateurs.utilisateur SET id_role = (SELECT id_role
--    FROM gestion_utilisateurs.role WHERE nom_role = 'ROLE_AGENT') WHERE email = '…';
ALTER TABLE gestion_utilisateurs.utilisateur ALTER COLUMN id_role SET NOT NULL;
ALTER TABLE gestion_utilisateurs.utilisateur ADD CONSTRAINT IF NOT EXISTS FK_utilisateur_role
    FOREIGN KEY (id_role) REFERENCES gestion_utilisateurs.role (id_role);

-- 4. Suppression des anciennes tables (dans l'ordre des dépendances)
DROP TABLE IF EXISTS gestion_utilisateurs.utilisateur_role;
DROP TABLE IF EXISTS gestion_utilisateurs.role_permission;
DROP TABLE IF EXISTS gestion_utilisateurs.permission;

-- 5. Contrôle final : doit renvoyer 0 ligne
SELECT email FROM gestion_utilisateurs.utilisateur WHERE id_role IS NULL;
