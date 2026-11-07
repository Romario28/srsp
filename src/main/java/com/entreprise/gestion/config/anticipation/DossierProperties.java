package com.entreprise.gestion.config.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "dossiers")
@Getter @Setter
public class DossierProperties {
    private LocalDate dateReprise;
    private Set<TypeAnticipation> typesSuivis = EnumSet.of(TypeAnticipation.DEPART_RETRAITE, TypeAnticipation.AVANCEMENT,
            TypeAnticipation.AVANCEMENT_CLASSE,
            TypeAnticipation.TITULARISATION, TypeAnticipation.FIN_CONTRAT, TypeAnticipation.INTEGRATION);

    // [NOUVEAU] rôles dispensés de la règle « pas d'action sur son propre dossier » ; vide = personne.
    // Exemple : dossiers.roles-exemptes-conflit=ROLE_ADMIN
    private Set<String> rolesExemptesConflit = new HashSet<>();
}
