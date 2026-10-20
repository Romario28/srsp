package com.entreprise.gestion.config.anticipation;

import com.entreprise.gestion.entite.anticipation.TypeAnticipation;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

@Component
@ConfigurationProperties(prefix="dossiers")
@Getter @Setter
public class DossierProperties {
    private LocalDate dateReprise;
    private int relanceApresJours=7;
    private Set<TypeAnticipation> typesSuivis=EnumSet.of(TypeAnticipation.DEPART_RETRAITE,TypeAnticipation.AVANCEMENT,
            TypeAnticipation.TITULARISATION,TypeAnticipation.FIN_CONTRAT,TypeAnticipation.INTEGRATION);
}
