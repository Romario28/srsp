package com.entreprise.gestion.entite.anticipation;

import java.io.Serializable;
import java.util.Objects;

public class IndiceGrdCorpsId implements Serializable {

    private static final long serialVersionUID = 1L;
    private String grade;
    private CorpsId corps;

    public IndiceGrdCorpsId() {}

    public IndiceGrdCorpsId(String grade, CorpsId corps) {
        this.grade = grade;
        this.corps = corps;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IndiceGrdCorpsId)) return false;
        IndiceGrdCorpsId that = (IndiceGrdCorpsId) o;
        return Objects.equals(grade, that.grade)
                && Objects.equals(corps, that.corps);
    }

    @Override
    public int hashCode() {
        return Objects.hash(grade, corps);
    }
}