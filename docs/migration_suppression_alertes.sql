-- ATTENTION : suppression définitive de l'historique des alertes (statuts, acquittements). Sauvegardez la base avant.
-- Sans objet en profil local (H2 en mémoire) : la table n'est plus créée.

-- 1. Information : nombre d'alertes qui seront perdues (échoue sans gravité si la table n'existe déjà plus)
SELECT COUNT(*) AS nb_alertes FROM anticipation.alerte;

-- 2. Suppression de la table
DROP TABLE IF EXISTS anticipation.alerte;
