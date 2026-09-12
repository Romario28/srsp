package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.CreateDepartementRequest;
import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.entite.Employe;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.DepartementRepository;
import com.entreprise.gestion.repository.EmployeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartementService {

    private final DepartementRepository departementRepository;
    private final EmployeRepository     employeRepository;

    @Transactional(readOnly = true)
    public List<Departement> findAll() { return departementRepository.findAll(); }

    public Departement findEntityById(Long id) {
        return departementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("DEPARTEMENT_INTROUVABLE", "Département introuvable : " + id));
    }

    /** Créé SANS chef — flux recommandé : département → employés rattachés → chef assigné. */
    @Transactional
    public Departement create(CreateDepartementRequest req) {
        Departement parent = req.getIdDepartementParent() != null
                ? findEntityById(req.getIdDepartementParent()) : null;

        Departement dept = Departement.builder()
                .nomDepartement(req.getNomDepartement()).niveau(req.getNiveau())
                .description(req.getDescription()).departementParent(parent).build();

        Departement saved = departementRepository.save(dept);
        saved.setChemin(calculerChemin(saved));
        return departementRepository.save(saved);
    }

    @Transactional
    @CacheEvict(value = "visibilityScope", allEntries = true)
    public Departement definirChef(Long idDepartement, Long idEmploye) {
        Departement dept = findEntityById(idDepartement);

        if (idEmploye == null) {
            dept.setChef(null);
            return departementRepository.save(dept);
        }

        Employe employe = employeRepository.findById(idEmploye)
                .orElseThrow(() -> new BusinessException("EMPLOYE_INTROUVABLE", "Employé introuvable : " + idEmploye));

        departementRepository.findByChefId(idEmploye).ifPresent(autreDept -> {
            if (!autreDept.getId().equals(idDepartement)) {
                throw new BusinessException("EMPLOYE_DEJA_CHEF",
                        "Cet employé est déjà chef du département : " + autreDept.getNomDepartement());
            }
        });

        dept.setChef(employe);
        return departementRepository.save(dept);
    }

    /** Déplace un département sous un nouveau parent, recalcule son chemin et celui de tous ses descendants. */
    @Transactional
    @CacheEvict(value = "visibilityScope", allEntries = true)
    public Departement deplacer(Long idDepartement, Long idNouveauParent) {
        Departement dept = findEntityById(idDepartement);
        Departement nouveauParent = idNouveauParent != null ? findEntityById(idNouveauParent) : null;

        if (nouveauParent != null && nouveauParent.getChemin().startsWith(dept.getChemin())) {
            throw new BusinessException("DEPLACEMENT_CYCLIQUE",
                    "Un département ne peut pas être déplacé sous l'un de ses propres descendants.");
        }

        String ancienChemin = dept.getChemin();
        dept.setDepartementParent(nouveauParent);
        dept.setChemin(calculerChemin(dept));
        departementRepository.save(dept);

        List<Departement> descendants = departementRepository.findByCheminStartingWith(ancienChemin);
        for (Departement d : descendants) {
            if (d.getId().equals(dept.getId())) continue;
            d.setChemin(dept.getChemin() + d.getChemin().substring(ancienChemin.length()));
        }
        departementRepository.saveAll(descendants);

        return dept;
    }

    private String calculerChemin(Departement d) {
        String base = d.getDepartementParent() != null ? d.getDepartementParent().getChemin() : "/";
        return base + d.getId() + "/";
    }
}