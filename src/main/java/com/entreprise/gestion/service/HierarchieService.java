package com.entreprise.gestion.service;

import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.entite.Employe;
import org.springframework.stereotype.Service;

/**
 * Règle centralisée, unique source de vérité :
 *  - si l'employé est chef de son département → manager = chef du parent
 *  - sinon → manager = chef de son propre département
 * Retourne null si aucun manager déterminable (pas de département,
 * racine sans parent, poste de chef vacant).
 */
@Service
public class HierarchieService {

    public Employe determinerManager(Employe employe) {
        Departement departement = employe.getDepartement();
        if (departement == null) return null;

        boolean estChef = departement.getChef() != null
                && departement.getChef().getId().equals(employe.getId());

        Departement reference = estChef ? departement.getDepartementParent() : departement;

        return reference != null ? reference.getChef() : null;
    }
}