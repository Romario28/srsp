-- 1. Compte : identité et lien informatif avec l'agent
ALTER TABLE gestion_utilisateurs.utilisateur ADD COLUMN IF NOT EXISTS nom VARCHAR(100);
ALTER TABLE gestion_utilisateurs.utilisateur ADD COLUMN IF NOT EXISTS prenom VARCHAR(100);
ALTER TABLE gestion_utilisateurs.utilisateur ADD COLUMN IF NOT EXISTS matricule_agent VARCHAR(20);
-- Si l'application a déjà créé cette contrainte au démarrage, cette ligne échoue : ignorez l'erreur.
ALTER TABLE gestion_utilisateurs.utilisateur ADD CONSTRAINT UQ_utilisateur_matricule_agent UNIQUE (matricule_agent);

-- 2. Reprise du nom (pas du matricule : celui d'un employé n'est pas celui d'un agent, aucun lien n'est créé)
UPDATE gestion_utilisateurs.utilisateur u
   SET nom    = (SELECT e.nom    FROM gestion_utilisateurs.employe e WHERE e.id_employe = u.id_employe),
       prenom = (SELECT e.prenom FROM gestion_utilisateurs.employe e WHERE e.id_employe = u.id_employe)
 WHERE u.id_employe IS NOT NULL;

-- 3. Rôle : ROLE_EMPLOYE devient ROLE_GESTIONNAIRE
UPDATE gestion_utilisateurs.role
   SET nom_role = 'ROLE_GESTIONNAIRE', description = 'Authentifié : périmètre défini par ses ministères, SOA et localités'
 WHERE nom_role = 'ROLE_EMPLOYE';

-- 4. Journal : libellé figé de l'acteur (les anciennes lignes restent à NULL : repli sur le libellé actuel du compte)
ALTER TABLE gestion_utilisateurs.log_audit ADD COLUMN IF NOT EXISTS acteur_libelle VARCHAR(255);

-- 5. Suppression de l'ancien modèle. La table perimetre_utilisateur est créée par l'application au démarrage.
ALTER TABLE gestion_utilisateurs.utilisateur DROP CONSTRAINT IF EXISTS FK_utilisateur_employe;
-- Si H2 refuse de supprimer la colonne, supprimez d'abord sa contrainte d'unicité (nom visible dans INFORMATION_SCHEMA).
ALTER TABLE gestion_utilisateurs.utilisateur DROP COLUMN IF EXISTS id_employe;
DROP TABLE IF EXISTS gestion_utilisateurs.portee_deleguee;
ALTER TABLE gestion_utilisateurs.departement DROP CONSTRAINT IF EXISTS FK_departement_chef;   -- dépendance circulaire
DROP TABLE IF EXISTS gestion_utilisateurs.employe;
DROP TABLE IF EXISTS gestion_utilisateurs.departement;

-- 6. Contrôle : les gestionnaires existants n'ont aucun périmètre, donc ne voient rien tant qu'un ADMIN ne l'a pas posé
SELECT u.email FROM gestion_utilisateurs.utilisateur u
 WHERE u.compte_systeme = FALSE
   AND NOT EXISTS (SELECT 1 FROM gestion_utilisateurs.perimetre_utilisateur p WHERE p.id_utilisateur = u.id_utilisateur);
