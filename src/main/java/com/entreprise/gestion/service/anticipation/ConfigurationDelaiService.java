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
                .map(c -> new FenetreAnticipation(c.getDelaiPrevenanceMois(), c.getDelaiRetardMois()))
                .orElseGet(() -> DelaisParDefaut.pour(type));
    }

    // MODIFIÉ — renommée : resoudreTous() → resoudreToutes(), retourne des FenetreAnticipation
    @Transactional(readOnly = true)
    public Map<TypeAnticipation, FenetreAnticipation> resoudreToutes() {
        return Arrays.stream(TypeAnticipation.values())
                .filter(t -> t != TypeAnticipation.ANOMALIE)
                .collect(Collectors.toMap(t -> t, this::resoudreFenetre));
    }

    /** Crée ou met à jour la fenêtre pour un type. */
    @Transactional
    public ConfigurationDelai definir(TypeAnticipation type, int prevenanceMois, int retardMois, UserDetailsImpl actor) {
        if (type == TypeAnticipation.ANOMALIE) {
            throw new BusinessException("TYPE_SANS_DELAI",
                    "Les anomalies n'ont pas de fenêtre configurable : elles sont toujours remontées.");
        }
        FenetreAnticipation.verifier(prevenanceMois, retardMois);

        FenetreAnticipation avant = resoudreFenetre(type);
        ConfigurationDelai config = configurationDelaiRepository.findById(type)
                .orElse(ConfigurationDelai.builder().type(type).build());
        config.setDelaiPrevenanceMois(prevenanceMois);
        config.setDelaiRetardMois(retardMois);
        config.setActif(true);
        ConfigurationDelai saved = configurationDelaiRepository.save(config);
        auditService.logAvecEntite(actor.getUtilisateur(), "UPDATE_CONFIG_DELAI",
                "Fenêtre « " + libelle(type) + " » : prévenance " + avant.prevenanceMois() + " → " + prevenanceMois
                        + " mois, retard " + avant.retardMois() + " → " + retardMois + " mois", null);
        return saved;
    }

    // reinitialiser() — inchangée
    @Transactional
    public void reinitialiser(TypeAnticipation type, UserDetailsImpl actor) {
        configurationDelaiRepository.findById(type).ifPresent(c -> {
            FenetreAnticipation defaut = DelaisParDefaut.pour(type);
            int prevenance = c.getDelaiPrevenanceMois();
            int retard = c.getDelaiRetardMois();
            configurationDelaiRepository.delete(c);
            auditService.logAvecEntite(actor.getUtilisateur(), "RESET_CONFIG_DELAI",
                    "Fenêtre « " + libelle(type) + " » : personnalisation retirée (prévenance "
                            + prevenance + " mois, retard " + retard + " mois), retour au défaut ("
                            + defaut.prevenanceMois() + " mois / " + defaut.retardMois() + " mois)", null);
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
