package com.entreprise.gestion.entite.anticipation;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CorpsId implements Serializable {
    private static final long serialVersionUID = 1L;
    private String code;
    private String categorie;
}
