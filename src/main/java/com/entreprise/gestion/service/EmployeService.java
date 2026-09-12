package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.CreateEmployeRequest;
import com.entreprise.gestion.dto.EmployeResponse;
import com.entreprise.gestion.dto.UpdateEmployeRequest;
import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.entite.Employe;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.DepartementRepository;
import com.entreprise.gestion.repository.EmployeRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.security.visibility.EmployeSpecifications;
import com.entreprise.gestion.security.visibility.VisibilityScope;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class EmployeService {

    private final EmployeRepository     employeRepository;
    private final DepartementRepository departementRepository;
    private final VisibiliteService     visibiliteService;
    private final HierarchieService     hierarchieService;
    private final AuditService          auditService;

    @Transactional(readOnly = true)
    public Page<EmployeResponse> findAllVisibles(UserDetailsImpl principal, Pageable pageable) {
        VisibilityScope scope = visibiliteService.resoudreScope(principal);
        if (scope.estVide()) return Page.empty(pageable);

        Set<Long> idsAvecCompte = employeRepository.findIdsWithUtilisateur();

        return employeRepository.findAll(EmployeSpecifications.visiblePar(scope), pageable)
                .map(e -> toResponse(e, idsAvecCompte.contains(e.getId())));
    }

    @Transactional(readOnly = true)
    public EmployeResponse findByIdVisible(Long id, UserDetailsImpl principal) {
        Employe employe = findEntityById(id);
        VisibilityScope scope = visibiliteService.resoudreScope(principal);

        String chemin = employe.getDepartement() != null ? employe.getDepartement().getChemin() : null;
        if (!scope.autoriseLecture(employe.getId(), chemin)) {
            throw new AccessDeniedException("Vous n'avez pas accès à la fiche de cet employé.");
        }
        return toResponse(employe, employe.getUtilisateur() != null);
    }

    @Transactional
    public EmployeResponse create(CreateEmployeRequest req, UserDetailsImpl actor) {
        if (employeRepository.existsByMatricule(req.getMatricule())) {
            throw new BusinessException("MATRICULE_DEJA_UTILISE", "Matricule déjà utilisé : " + req.getMatricule());
        }

        Departement departement = departementRepository.findById(req.getIdDepartement())
                .orElseThrow(() -> new BusinessException("DEPARTEMENT_INTROUVABLE",
                        "Département introuvable : " + req.getIdDepartement()));

        VisibilityScope scope = visibiliteService.resoudreScope(actor);
        if (!scope.autoriseEcriture(null, departement.getChemin())) {
            throw new AccessDeniedException("Vous n'avez pas les droits d'écriture sur ce département.");
        }

        Employe employe = Employe.builder()
                .matricule(req.getMatricule()).nom(req.getNom()).prenom(req.getPrenom())
                .poste(req.getPoste()).dateEmbauche(req.getDateEmbauche()).departement(departement).build();

        Employe saved = employeRepository.save(employe);
        auditService.logAvecEntite(actor.getUtilisateur(), "CREATE_EMPLOYE",
                "Matricule : " + saved.getMatricule() + " par " + actor.getNomComplet(), null);

        return toResponse(saved, false);
    }

    @Transactional
    @CacheEvict(value = "visibilityScope", allEntries = true)
    public EmployeResponse update(Long id, UpdateEmployeRequest req, UserDetailsImpl actor) {
        Employe employe = findEntityById(id);
        VisibilityScope scope = visibiliteService.resoudreScope(actor);

        String cheminActuel = employe.getDepartement() != null ? employe.getDepartement().getChemin() : null;
        if (!scope.autoriseEcriture(employe.getId(), cheminActuel)) {
            throw new AccessDeniedException("Vous n'avez pas les droits d'écriture sur cet employé.");
        }

        if (req.getNom()          != null) employe.setNom(req.getNom());
        if (req.getPrenom()       != null) employe.setPrenom(req.getPrenom());
        if (req.getPoste()        != null) employe.setPoste(req.getPoste());
        if (req.getDateEmbauche() != null) employe.setDateEmbauche(req.getDateEmbauche());

        if (req.getIdDepartement() != null) {
            Departement nouveauDept = departementRepository.findById(req.getIdDepartement())
                    .orElseThrow(() -> new BusinessException("DEPARTEMENT_INTROUVABLE",
                            "Département introuvable : " + req.getIdDepartement()));
            // Il faut aussi l'écriture sur la DESTINATION : on ne peut pas
            // déplacer un employé vers un sous-arbre qu'on ne contrôle pas.
            if (!scope.autoriseEcriture(null, nouveauDept.getChemin())) {
                throw new AccessDeniedException("Vous n'avez pas les droits d'écriture sur le département de destination.");
            }
            employe.setDepartement(nouveauDept);
        }

        Employe saved = employeRepository.save(employe);
        auditService.logAvecEntite(actor.getUtilisateur(), "UPDATE_EMPLOYE",
                "Id : " + id + " par " + actor.getNomComplet(), null);

        return toResponse(saved, saved.getUtilisateur() != null);
    }

    @Transactional
    public void delete(Long id, UserDetailsImpl actor) {
        Employe employe = findEntityById(id);

        VisibilityScope scope = visibiliteService.resoudreScope(actor);
        String chemin = employe.getDepartement() != null ? employe.getDepartement().getChemin() : null;
        if (!scope.autoriseEcriture(employe.getId(), chemin)) {
            throw new AccessDeniedException("Vous n'avez pas les droits d'écriture sur cet employé.");
        }

        if (employeRepository.hasUtilisateur(id)) {
            throw new BusinessException("EMPLOYE_A_UN_COMPTE",
                    "Impossible : cet employé possède un compte. Désactivez-le d'abord.");
        }


        employeRepository.deleteById(id);
        auditService.logAvecEntite(actor.getUtilisateur(), "DELETE_EMPLOYE",
                "Id : " + id + " par " + actor.getNomComplet(), null);
    }

    public Employe findEntityById(Long id) {
        return employeRepository.findById(id)
                .orElseThrow(() -> new BusinessException("EMPLOYE_INTROUVABLE", "Employé introuvable : " + id));
    }

    private EmployeResponse toResponse(Employe e, boolean aUnCompte) {
        EmployeResponse dto = EmployeResponse.from(e);
        dto.setAUnCompte(aUnCompte);

        if (e.getDepartement() != null) {
            dto.setIdDepartement(e.getDepartement().getId());
            dto.setNomDepartement(e.getDepartement().getNomDepartement());
        }

        Employe manager = hierarchieService.determinerManager(e);
        if (manager != null) {
            dto.setIdManager(manager.getId());
            dto.setNomManager(manager.getPrenom() + " " + manager.getNom());
        }

        return dto;
    }
}