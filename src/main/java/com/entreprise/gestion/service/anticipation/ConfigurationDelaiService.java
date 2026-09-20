package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.ConfigurationDelaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Point unique de résolution des fenêtres d'anticipation : configuration active en
 * base si elle existe, sinon couple prévenance/retard par défaut
 * ({@link FenetresParDefaut}).
 *
 * <p>Le batch nocturne et les endpoints API passent tous par ce service : les deux
 * voies appliquent donc rigoureusement la même règle.</p>
 */
@Service
@RequiredArgsConstructor
public class ConfigurationDelaiService {

    private final ConfigurationDelaiRepository configurationDelaiRepository;

    /** Fenêtre effective d'un type : surcharge active en base, sinon défaut. */
    @Transactional(readOnly = true)
    public FenetreAnticipation resoudrePlage(TypeAnticipation type) {
        return appliquer(FenetresParDefaut.pour(type),
                configurationDelaiRepository.findById(type).orElse(null));
    }

    /** Prévenance effective (borne haute) — raccourci pour les appelants qui n'utilisent que celle-ci. */
    @Transactional(readOnly = true)
    public int resoudreDelai(TypeAnticipation type) {
        return resoudrePlage(type).prevenanceJours();
    }

    /**
     * Fenêtres effectives de tous les types configurables, dans l'ordre de l'énumération.
     * Une seule requête : c'est la méthode utilisée par le batch pour résoudre l'ensemble
     * des fenêtres avant de parcourir les agents.
     */
    @Transactional(readOnly = true)
    public Map<TypeAnticipation, FenetreAnticipation> resoudreToutes() {
        Map<TypeAnticipation, ConfigurationDelai> surcharges = new EnumMap<>(TypeAnticipation.class);
        for (ConfigurationDelai config : configurationDelaiRepository.findByActifTrue()) {
            surcharges.put(config.getType(), config);
        }

        Map<TypeAnticipation, FenetreAnticipation> resultat = new LinkedHashMap<>();
        for (TypeAnticipation type : FenetresParDefaut.typesConfigurables()) {
            resultat.put(type, appliquer(FenetresParDefaut.pour(type), surcharges.get(type)));
        }
        return resultat;
    }

    /** Vrai si une surcharge active existe en base pour ce type. */
    @Transactional(readOnly = true)
    public boolean estPersonnalise(TypeAnticipation type) {
        return configurationDelaiRepository.findById(type)
                .filter(ConfigurationDelai::isActif)
                .isPresent();
    }

    /**
     * Crée ou met à jour la surcharge d'un type.
     *
     * <p>Une borne non fournie ({@code null}) conserve la valeur déjà enregistrée dans la
     * ligne, ou à défaut le défaut du type : la ligne enregistrée est ainsi auto-suffisante
     * (les deux bornes y figurent explicitement).</p>
     */
    @Transactional
    public ConfigurationDelai definir(TypeAnticipation type,
                                      Integer prevenanceJours,
                                      Integer retardJours,
                                      Boolean actif) {
        verifierTypeConfigurable(type);
        verifierBornes(prevenanceJours, retardJours);

        FenetreAnticipation defaut = FenetresParDefaut.pour(type);
        ConfigurationDelai existante = configurationDelaiRepository.findById(type).orElse(null);

        // Référence pour les bornes non fournies : la ligne existante (même désactivée,
        // c'est le dernier paramétrage connu), sinon le défaut du type.
        FenetreAnticipation reference = (existante != null)
                ? new FenetreAnticipation(
                        borne(existante.getDelaiPrevenanceJours(), defaut.prevenanceJours()),
                        borne(existante.getRetardJours(), defaut.retardJours()))
                : defaut;

        ConfigurationDelai config = (existante != null)
                ? existante
                : ConfigurationDelai.builder().type(type).build();

        config.setDelaiPrevenanceJours(prevenanceJours != null ? prevenanceJours : reference.prevenanceJours());
        config.setRetardJours(retardJours != null ? retardJours : reference.retardJours());
        config.setActif(actif == null || actif);
        return configurationDelaiRepository.save(config);
    }

    /** Retire la surcharge : le type retombe automatiquement sur ses valeurs par défaut. */
    @Transactional
    public void reinitialiser(TypeAnticipation type) {
        verifierTypeConfigurable(type);
        configurationDelaiRepository.findById(type).ifPresent(configurationDelaiRepository::delete);
    }

    // ─── Résolution ─────────────────────────────────────────────────────────

    /**
     * Applique la surcharge (si elle existe et est active) sur le couple par défaut du
     * type. Une borne absente en base est considérée comme non renseignée et retombe sur
     * le défaut correspondant — cas des lignes créées avant l'introduction du retard.
     */
    private FenetreAnticipation appliquer(FenetreAnticipation defaut, ConfigurationDelai surcharge) {
        if (surcharge == null || !surcharge.isActif()) {
            return defaut;
        }
        return new FenetreAnticipation(
                borne(surcharge.getDelaiPrevenanceJours(), defaut.prevenanceJours()),
                borne(surcharge.getRetardJours(), defaut.retardJours()));
    }

    // ─── Garde-fous ─────────────────────────────────────────────────────────

    private void verifierTypeConfigurable(TypeAnticipation type) {
        if (!FenetresParDefaut.estConfigurable(type)) {
            throw new BusinessException("TYPE_SANS_FENETRE",
                    "Ce type d'anticipation n'a pas de fenêtre configurable : "
                            + "les anomalies sont toujours remontées.");
        }
    }

    private void verifierBornes(Integer prevenanceJours, Integer retardJours) {
        if (prevenanceJours != null && prevenanceJours < 0) {
            throw new BusinessException("PREVENANCE_INVALIDE",
                    "La prévenance doit être positive ou nulle.");
        }
        if (retardJours != null && retardJours < 0) {
            throw new BusinessException("RETARD_INVALIDE",
                    "Le retard doit être positif ou nul (0 = ne pas afficher les retards).");
        }
    }

    /**
     * Valeur lue en base : {@code null} (ligne héritée) → défaut du type ; valeur négative
     * saisie directement en base → ramenée à 0 plutôt que de faire échouer le calcul.
     */
    private int borne(Integer valeurEnBase, int defaut) {
        if (valeurEnBase == null) {
            return defaut;
        }
        return Math.max(0, valeurEnBase);
    }
}
