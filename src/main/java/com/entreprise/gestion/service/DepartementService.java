package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.DepartementDTO;
import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.DepartementRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartementService {

    private static final int LIBELLE_MAX = 200;

    private final DepartementRepository departementRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<DepartementDTO> lister() {
        return departementRepository.findAllByOrderByLibelleAsc().stream().map(DepartementService::versDto).toList();
    }

    @Transactional
    public DepartementDTO creer(String libelle, UserDetailsImpl acteur) {
        String propre = nettoyer(libelle);
        if (departementRepository.existsByLibelleIgnoreCase(propre)) throw dejaExistant(propre);
        Departement d = departementRepository.save(Departement.builder().libelle(propre).build());
        auditService.logAvecEntite(acteur.getUtilisateur(), "CREATE_DEPARTEMENT",
                "Département créé : « " + propre + " » — par " + acteur.getNomComplet(), null);
        return versDto(d);
    }

    @Transactional
    public DepartementDTO modifier(Long id, String libelle, UserDetailsImpl acteur) {
        Departement d = trouver(id);
        String propre = nettoyer(libelle);
        if (departementRepository.existsByLibelleIgnoreCaseAndIdNot(propre, id)) throw dejaExistant(propre);
        String ancien = d.getLibelle();
        d.setLibelle(propre);
        departementRepository.save(d);
        auditService.logAvecEntite(acteur.getUtilisateur(), "UPDATE_DEPARTEMENT",
                "Département modifié : « " + ancien + " » → « " + propre + " » — par " + acteur.getNomComplet(), null);
        return versDto(d);
    }

    @Transactional
    public void supprimer(Long id, UserDetailsImpl acteur) {
        Departement d = trouver(id);
        long n = utilisateurRepository.countByDepartementId(id);
        if (n > 0) {
            throw new BusinessException("DEPARTEMENT_UTILISE", "Ce département est celui de " + n
                    + (n > 1 ? " comptes ou demandes" : " compte ou demande")
                    + " : suppression impossible. Vous pouvez le renommer.");
        }
        departementRepository.delete(d);
        auditService.logAvecEntite(acteur.getUtilisateur(), "DELETE_DEPARTEMENT",
                "Département supprimé : « " + d.getLibelle() + " » — par " + acteur.getNomComplet(), null);
    }

    private Departement trouver(Long id) {
        return departementRepository.findById(id)
                .orElseThrow(() -> new BusinessException("DEPARTEMENT_INTROUVABLE", "Département introuvable : " + id));
    }

    private static BusinessException dejaExistant(String libelle) {
        return new BusinessException("DEPARTEMENT_EXISTANT", "Un département porte déjà ce libellé : « " + libelle + " ».");
    }

    private static String nettoyer(String s) {
        String t = s == null ? "" : s.trim().replaceAll("\\s+", " ");
        if (t.isEmpty()) throw new BusinessException("LIBELLE_REQUIS", "Le libellé est obligatoire.");
        if (t.length() > LIBELLE_MAX) {
            throw new BusinessException("LIBELLE_TROP_LONG", "Le libellé ne peut pas dépasser " + LIBELLE_MAX + " caractères.");
        }
        return t;
    }

    private static DepartementDTO versDto(Departement d) { return new DepartementDTO(d.getId(), d.getLibelle()); }
}
