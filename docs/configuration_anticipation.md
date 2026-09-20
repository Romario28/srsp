# Configuration des fenêtres d'anticipation (prévenance / retard)

Module : `service/anticipation` — paramétrage des échéances de carrière remontées par
l'API et par le batch nocturne.

## 1. Principe : une fenêtre à deux bornes

Chaque type d'anticipation applique une fenêtre exprimée en jours autour de la date
d'échéance de l'agent. `joursRestants` est l'écart entre aujourd'hui et l'échéance :

| Situation | `joursRestants` |
|---|---|
| Échéance à venir (préavis) | `> 0` |
| Échéance du jour | `0` |
| Échéance dépassée (retard à régulariser) | `< 0` |

```
        échéance
           │
  ─────────┼───────────────────────────────┼────────────→
      -retard                          +prevenance
   (borne basse)                      (borne haute)

  agent visible  ⇔  joursRestants ∈ [-retard ; +prevenance]
```

* **Prévenance** (borne haute) : nombre de jours *avant* l'échéance à partir duquel
  l'agent commence à apparaître. `120` = prévenir 4 mois avant.
* **Retard** (borne basse) : nombre de jours *après* l'échéance au-delà duquel l'agent
  n'apparaît plus. `30` = ne remonter que les dépassements de moins d'un mois.
  `0` = ne jamais remonter les retards (uniquement la prévenance).

## 2. Valeurs par défaut (codées en dur)

| Type | Prévenance | Retard | Justification |
|---|---:|---:|---|
| `DEPART_RETRAITE` | 548 j (18 mois) | 365 j | Préavis large pour préparer le dossier de pension ; un départ dépassé reste visible un an (régularisation). |
| `AVANCEMENT` | 90 j | 365 j | Régularisation d'un avancement manqué avec effet rétroactif. |
| `TITULARISATION` | 90 j | 365 j | Idem avancement. |
| `FIN_CONTRAT` | 90 j | 30 j | Au-delà d'un mois après la fin du CDD, le renouvellement n'est plus réalisable. |
| `ANOMALIE` | *aucune* | *aucune* | Une anomalie de donnée n'a pas d'échéance : toujours remontée. |

Les défauts sont définis dans **`FenetresParDefaut`** (unique point à modifier dans le
code, constantes simples et documentées).

## 3. Règle de résolution (une seule, partagée)

Implémentée par `ConfigurationDelaiService.resoudrePlage(type)` :

1. si une ligne **active** existe dans `anticipation.configuration_delai` pour ce type,
   ses deux bornes sont utilisées ;
2. sinon, on retombe sur le couple par défaut de `FenetresParDefaut`.

Cas particuliers :

* une ligne héritée sans colonne `retard_jours` (non renseignée) → le **retard par défaut
  du type** s'applique, la prévenance enregistrée est conservée ; aucune migration
  bloquante ;
* une borne négative saisie directement en base est ramenée à `0` (le calcul ne casse pas) ;
* une ligne avec `actif = false` est ignorée → retour au défaut.

Le batch nocturne (`BatchAnticipation`) et les endpoints API (`AnticipationController`)
appellent tous les deux `ConfigurationDelaiService` : ils appliquent donc strictement la
même règle, sans duplication de logique.

## 4. API de configuration

| Méthode | Chemin | Rôle |
|---|---|---|
| `GET` | `/api/anticipation/configuration` | Bornes effectives + valeurs par défaut + `personnalise` |
| `PUT` | `/api/anticipation/configuration/{type}` | Créer / modifier la surcharge |
| `DELETE` | `/api/anticipation/configuration/{type}` | Réinitialiser (retour au défaut) |

`{type}` ∈ `DEPART_RETRAITE`, `AVANCEMENT`, `TITULARISATION`, `FIN_CONTRAT`.
`GET` est ouvert à `ADMIN` et `EMPLOYE` ; `PUT` et `DELETE` sont réservés à `ADMIN`.

Exemples :

```bash
# Prévenir 4 mois avant, ne garder les retards que 1 mois
curl -X PUT /api/anticipation/configuration/DEPART_RETRAITE -H 'Content-Type: application/json' \
     -d '{"delaiPrevenanceJours":120,"retardJours":30}'

# Ne modifier que la prévenance : le retard déjà paramétré est conservé
curl -X PUT /api/anticipation/configuration/AVANCEMENT -H 'Content-Type: application/json' \
     -d '{"delaiPrevenanceJours":60}'

# Retour au défaut codé en dur
curl -X DELETE /api/anticipation/configuration/AVANCEMENT

# Désactiver une surcharge sans la supprimer
curl -X PUT /api/anticipation/configuration/FIN_CONTRAT -H 'Content-Type: application/json' \
     -d '{"retardJours":15,"actif":false}'
```

Réponse (`GET`, extrait) :

```json
[
  {
    "type": "DEPART_RETRAITE",
    "delaiPrevenanceJours": 120,
    "delaiParDefaut": 548,
    "retardJours": 30,
    "retardParDefaut": 365,
    "personnalise": true
  }
]
```

## 5. Consultation avec surcharge ponctuelle

Les endpoints de consultation acceptent `prevenanceJours` et `retardJours` pour élargir
ou resserrer la fenêtre le temps d'une requête (sans modifier la configuration) :

```bash
GET /api/anticipation/retraite                       # fenêtre configurée (base ou défaut)
GET /api/anticipation/retraite?retardJours=0         # uniquement les échéances à venir
GET /api/anticipation/avancement?prevenanceJours=365 # préavis élargi à un an
```

`horizonJours` reste accepté comme alias de l'ancienne borne de prévenance (compatibilité).

Réponse : `joursRestants` positif pour une échéance à venir, négatif pour un retard.

## 6. Base de données

Table `anticipation.configuration_delai` :

| Colonne | Type | Rôle |
|---|---|---|
| `type_anticipation` | `VARCHAR(30)` (PK) | `DEPART_RETRAITE`, `AVANCEMENT`, `TITULARISATION`, `FIN_CONTRAT` |
| `delai_prevenance_jours` | `INTEGER` | borne haute (jours avant l'échéance) |
| `retard_jours` | `INTEGER` | borne basse (jours après l'échéance) — `NULL` = retard par défaut du type |
| `actif` | `BOOLEAN NOT NULL` | `false` → ligne ignorée |

Toutes les valeurs sont modifiables directement en SQL, sans redéploiement — voir
`docs/migration_configuration_anticipation.sql` pour une base déjà en service.

## 7. Cycle de vie des alertes

Le batch **crée** une alerte dès que l'échéance entre dans la fenêtre. Une alerte déjà
créée est conservée même si l'échéance sort ensuite de la fenêtre (retard trop ancien) :
elle reste consultable dans `/api/alertes` avec son statut (`NOUVELLE`, `VUE`,
`ACQUITTEE`) — l'historique et les acquittements ne sont jamais perdus.
