-- 1. Droit de l'agent sur son propre dossier (faux par défaut)
ALTER TABLE gestion_utilisateurs.utilisateur ADD COLUMN IF NOT EXISTS agir_sur_son_dossier BOOLEAN DEFAULT FALSE NOT NULL;

-- 2. Les anciens gestionnaires deviennent des utilisateurs RH (ils gardent leur périmètre).
--    ROLE_CHEF et ROLE_AGENT sont créés au démarrage par DataInitializer.
UPDATE gestion_utilisateurs.role
   SET nom_role = 'ROLE_RH', description = 'Utilisateur RH : anticipation dans son périmètre'
 WHERE nom_role = 'ROLE_GESTIONNAIRE';

-- 3. Contrôle : chaque compte (hors système) doit avoir exactement un profil connu. Doit renvoyer 0 ligne.
SELECT u.email, COUNT(r.id_role) AS nb_roles
  FROM gestion_utilisateurs.utilisateur u
  LEFT JOIN gestion_utilisateurs.utilisateur_role ur ON ur.id_utilisateur = u.id_utilisateur
  LEFT JOIN gestion_utilisateurs.role r ON r.id_role = ur.id_role
   AND r.nom_role IN ('ROLE_ADMIN', 'ROLE_CHEF', 'ROLE_RH', 'ROLE_AGENT')
 WHERE u.compte_systeme = FALSE
 GROUP BY u.email
HAVING COUNT(r.id_role) <> 1;

-- 4. Information : comptes RH ou Chef sans aucune valeur de périmètre (ils ne voient aucun agent)
SELECT u.email FROM gestion_utilisateurs.utilisateur u
 WHERE u.compte_systeme = FALSE
   AND NOT EXISTS (SELECT 1 FROM gestion_utilisateurs.perimetre_utilisateur p WHERE p.id_utilisateur = u.id_utilisateur);
