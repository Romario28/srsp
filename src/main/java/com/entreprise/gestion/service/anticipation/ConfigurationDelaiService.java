package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.ConfigurationDelaiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConfigurationDelaiService {

    private final ConfigurationDelaiRepository configurationDelaiRepository;

    /** Délai effectif : ligne DB active si elle existe, sinon valeur par défaut codée en dur. */
    @Transactional(readOnly = true)
    public int resoudreDelai(TypeAnticipation type) {
        return configurationDelaiRepository.findById(type)
                .filter(ConfigurationDelai::isActif)
                .map(ConfigurationDelai::getDelaiPrevenanceJours)
                .orElse(DelaisParDefaut.pour(type));
    }

    @Transactional(readOnly = true)
    public Map<TypeAnticipation, Integer> resoudreTous() {
        return Arrays.stream(TypeAnticipation.values())
                .filter(t -> t != TypeAnticipation.ANOMALIE)
                .collect(Collectors.toMap(t -> t, this::resoudreDelai));
    }

    /** Crée ou met à jour la surcharge pour un type. */
    @Transactional
    public ConfigurationDelai definir(TypeAnticipation type, int delaiJours) {
        if (type == TypeAnticipation.ANOMALIE) {
            throw new BusinessException("TYPE_SANS_DELAI",
                    "Les anomalies n'ont pas de délai de prévenance : elles sont toujours remontées.");
        }
        if (delaiJours < 0) {
            throw new BusinessException("DELAI_INVALIDE", "Le délai de prévenance doit être positif ou nul.");
        }

        ConfigurationDelai config = configurationDelaiRepository.findById(type)
                .orElse(ConfigurationDelai.builder().type(type).build());
        config.setDelaiPrevenanceJours(delaiJours);
        config.setActif(true);
        return configurationDelaiRepository.save(config);
    }

    /** Retire la surcharge : le type retombe automatiquement sur sa valeur par défaut. */
    @Transactional
    public void reinitialiser(TypeAnticipation type) {
        configurationDelaiRepository.findById(type).ifPresent(configurationDelaiRepository::delete);
    }
}