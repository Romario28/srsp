package com.entreprise.gestion.service.anticipation;

import com.entreprise.gestion.entite.anticipation.ConfigurationDelai;
import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.anticipation.ConfigurationDelaiRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.service.AuditService;
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
    private final AuditService auditService;

    // MODIFIÉ — renommée : resoudreDelai(type):int → resoudreFenetre(type):FenetreAnticipation
    /** Fenêtre effective : ligne DB active si elle existe, sinon la fenêtre par défaut. */
    @Transactional(readOnly = true)
    public FenetreAnticipation resoudreFenetre(TypeAnticipation type) {
        return configurationDelaiRepository.findById(type)
                .filter(ConfigurationDelai::isActif)
                .map(c -> new FenetreAnticipation(c.getDelaiPrevenanceJours(), c.getDelaiRetardJours())) // MODIFIÉ
                .orElseGet(() -> DelaisParDefaut.pour(type));
    }

    // MODIFIÉ — renommée : resoudreTous() → resoudreToutes(), retourne des FenetreAnticipation
    @Transactional(readOnly = true)
    public Map<TypeAnticipation, FenetreAnticipation> resoudreToutes() {
        return Arrays.stream(TypeAnticipation.values())
                .filter(t -> t != TypeAnticipation.ANOMALIE)
                .collect(Collectors.toMap(t -> t, this::resoudreFenetre));
    }

    // MODIFIÉ — signature : definir(type, int delaiJours) → definir(type, int prevenanceJours, int retardJours)
    /** Crée ou met à jour la fenêtre pour un type. */
    @Transactional
    public ConfigurationDelai definir(TypeAnticipation type, int prevenanceJours, int retardJours, UserDetailsImpl actor) {
        if (type == TypeAnticipation.ANOMALIE) {
            throw new BusinessException("TYPE_SANS_DELAI",
                    "Les anomalies n'ont pas de fenêtre configurable : elles sont toujours remontées.");
        }
        if (prevenanceJours < 0 || retardJours < 0) {   // MODIFIÉ — valide les deux bornes
            throw new BusinessException("DELAI_INVALIDE", "Les délais doivent être positifs ou nuls.");
        }

        FenetreAnticipation avant = resoudreFenetre(type);
        ConfigurationDelai config = configurationDelaiRepository.findById(type)
                .orElse(ConfigurationDelai.builder().type(type).build());
        config.setDelaiPrevenanceJours(prevenanceJours);
        config.setDelaiRetardJours(retardJours);   // AJOUTÉ
        config.setActif(true);
        ConfigurationDelai saved = configurationDelaiRepository.save(config);
        auditService.logAvecEntite(actor.getUtilisateur(), "UPDATE_CONFIG_DELAI",
                "Fenêtre « " + libelle(type) + " » : prévenance " + avant.prevenanceJours() + " → " + prevenanceJours
                        + " j, retard " + avant.retardJours() + " → " + retardJours + " j", null);
        return saved;
    }

    // reinitialiser() — inchangée
    @Transactional
    public void reinitialiser(TypeAnticipation type, UserDetailsImpl actor) {
        configurationDelaiRepository.findById(type).ifPresent(c -> {
            FenetreAnticipation defaut = DelaisParDefaut.pour(type);
            int prevenance = c.getDelaiPrevenanceJours();
            int retard = c.getDelaiRetardJours();
            configurationDelaiRepository.delete(c);
            auditService.logAvecEntite(actor.getUtilisateur(), "RESET_CONFIG_DELAI",
                    "Fenêtre « " + libelle(type) + " » : personnalisation retirée (prévenance "
                            + prevenance + " j, retard " + retard + " j), retour au défaut ("
                            + defaut.prevenanceJours() + " j / " + defaut.retardJours() + " j)", null);
        });
    }

    private static String libelle(TypeAnticipation type) {
        return switch (type) {
            case DEPART_RETRAITE -> "Départ à la retraite";
            case AVANCEMENT -> "Avancement";
            case TITULARISATION -> "Titularisation";
            case FIN_CONTRAT -> "Fin de contrat";
            case ANOMALIE -> "Anomalie";
        };
    }
}
