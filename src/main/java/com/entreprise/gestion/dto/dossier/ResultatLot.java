package com.entreprise.gestion.dto.dossier;
import java.util.List;
public record ResultatLot(int modifies,int inchanges,List<Refus> refuses) {
    public record Refus(Long id,String libelle,String motif) {}
}
