package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.ConfigurationDelaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Point de résolution UNIQUE des fenêtres d'anticipation :
 * surcharge en base si elle existe et est active, sinon couple par défaut codé en dur.
 *
 * Le batch nocturne ({@link BatchAnticipation}) et les endpoints API
 * ({@link AnticipationService}) passent tous par ici — aucune autre règle de visibilité
 * n'est dupliquée ailleurs.
 */
@Service
@RequiredArgsConstructor
public class ConfigurationDelaiService {

    private final ConfigurationDelaiRepository configurationDelaiRepository;

    /** Fenêtre effective d'un type : ligne DB active si elle existe, sinon couple par défaut. */
    @Transactional(readOnly = true)
    public FenetreAnticipation resoudreFenetre(TypeAnticipation type) {
        FenetreAnticipation defaut = FenetresParDefaut.pour(type);
        return configurationDelaiRepository.findById(type)
                .filter(ConfigurationDelai::isActif)
                .map(config -> versFenetre(config, defaut))
                .orElse(defaut);
    }

    /**
     * Toutes les fenêtres effectives des types configurables (ANOMALIE exclue).
     * Permet au batch de ne résoudre qu'une fois par exécution.
     */
    @Transactional(readOnly = true)
    public Map<TypeAnticipation, FenetreAnticipation> resoudreToutes() {
        return Arrays.stream(TypeAnticipation.values())
                .filter(FenetresParDefaut::configurable)
                .collect(Collectors.toMap(
                        t -> t,
                        this::resoudreFenetre,
                        (a, b) -> a,
                        LinkedHashMap::new));
    }

    /** La ligne en base pour ce type, active ou non — {@code null} si aucune surcharge n'existe. */
    @Transactional(readOnly = true)
    public ConfigurationDelai configurationEnBase(TypeAnticipation type) {
        return configurationDelaiRepository.findById(type).orElse(null);
    }

    /**
     * Crée ou met à jour la surcharge d'un type.
     *
     * @param prevenanceJours borne « plus tôt » ; {@code null} = conserver la prévenance effective
     * @param retardJours     borne « plus tard » ; {@code null} = conserver le retard effectif
     * @param actif           {@code false} pour neutraliser la surcharge sans la supprimer ;
     *                        {@code null} = (ré)activer la surcharge
     */
    @Transactional
    public ConfigurationDelai definir(TypeAnticipation type,
                                      Integer prevenanceJours,
                                      Integer retardJours,
                                      Boolean actif) {
        if (!FenetresParDefaut.configurable(type)) {
            throw new BusinessException("TYPE_SANS_FENETRE",
                    "Ce type d'anticipation n'a pas de fenêtre configurable : " + type
                            + " (une anomalie est toujours remontée).");
        }

        FenetreAnticipation courante = resoudreFenetre(type);
        int prevenance = prevenanceJours != null ? prevenanceJours : courante.prevenanceJours();
        int retard = retardJours != null ? retardJours : courante.retardJours();
        validerBornes(prevenance, retard);

        ConfigurationDelai config = configurationDelaiRepository.findById(type)
                .orElseGet(() -> ConfigurationDelai.builder().type(type).build());
        config.setDelaiPrevenanceJours(prevenance);
        config.setRetardJours(retard);
        config.setActif(actif == null ? true : actif);
        return configurationDelaiRepository.save(config);
    }

    /** Retire la surcharge : le type retombe immédiatement sur son couple par défaut. */
    @Transactional
    public void reinitialiser(TypeAnticipation type) {
        configurationDelaiRepository.findById(type).ifPresent(configurationDelaiRepository::delete);
    }

    /**
     * Traduit une ligne en fenêtre exploitable. Les colonnes nulles (lignes héritées d'avant
     * l'ajout de la borne de retard) retombent sur la valeur par défaut correspondante.
     */
    private FenetreAnticipation versFenetre(ConfigurationDelai config, FenetreAnticipation defaut) {
        int prevenance = config.getDelaiPrevenanceJours() != null
                ? config.getDelaiPrevenanceJours()
                : defaut.prevenanceJours();
        int retard = config.getRetardJours() != null
                ? config.getRetardJours()
                : defaut.retardJours();
        return new FenetreAnticipation(prevenance, retard);
    }

    private void validerBornes(int prevenanceJours, int retardJours) {
        if (prevenanceJours < 0) {
            throw new BusinessException("PREVENANCE_INVALIDE",
                    "La prévenance doit être positive ou nulle.");
        }
        if (retardJours < 0) {
            throw new BusinessException("RETARD_INVALIDE",
                    "Le retard doit être positif ou nul.");
        }
    }
}
