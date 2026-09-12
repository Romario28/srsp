package com.entreprise.gestion.service;

import com.entreprise.gestion.dto.CreatePorteeDelegueeRequest;
import com.entreprise.gestion.dto.PorteeDelegueeDTO;
import com.entreprise.gestion.entite.Departement;
import com.entreprise.gestion.entite.PorteeDeleguee;
import com.entreprise.gestion.entite.Utilisateur;
import com.entreprise.gestion.exception.BusinessException;
import com.entreprise.gestion.repository.DepartementRepository;
import com.entreprise.gestion.repository.PorteeDelegueeRepository;
import com.entreprise.gestion.repository.UtilisateurRepository;
import com.entreprise.gestion.security.UserDetailsImpl;
import com.entreprise.gestion.security.visibility.VisibilityScope;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Qui peut déléguer une portée sur un département ? Quiconque a déjà
 * l'écriture dessus — ADMIN, chef du département (ou d'un ancêtre), ou
 * détenteur d'une PorteeDeleguee LECTURE_ECRITURE qui le couvre déjà.
 * L'autorité de délégation découle de l'autorité elle-même : pas de rôle
 * dédié, pas de liste blanche séparée à maintenir.
 */
@Service
@RequiredArgsConstructor
public class PorteeDelegueeService {

    private final PorteeDelegueeRepository porteeDelegueeRepository;
    private final UtilisateurRepository    utilisateurRepository;
    private final DepartementRepository    departementRepository;
    private final VisibiliteService        visibiliteService;
    private final CacheManager cacheManager;

    @Transactional
    @CacheEvict(value = "visibilityScope", key = "#req.idUtilisateur")
    public PorteeDelegueeDTO accorder(CreatePorteeDelegueeRequest req, UserDetailsImpl actor) {

        Departement departement = departementRepository.findById(req.getIdDepartement())
                .orElseThrow(() -> new BusinessException("DEPARTEMENT_INTROUVABLE",
                        "Département introuvable : " + req.getIdDepartement()));

        VisibilityScope scopeActor = visibiliteService.resoudreScope(actor);
        if (!scopeActor.autoriseEcriture(null, departement.getChemin())) {
            throw new AccessDeniedException("Vous n'avez pas l'autorité pour déléguer un accès sur ce département.");
        }

        Utilisateur beneficiaire = utilisateurRepository.findById(req.getIdUtilisateur())
                .orElseThrow(() -> new BusinessException("UTILISATEUR_INTROUVABLE",
                        "Utilisateur introuvable : " + req.getIdUtilisateur()));

        if (req.getDateFin() != null && req.getDateFin().isBefore(req.getDateDebut())) {
            throw new BusinessException("DATES_INVALIDES", "La date de fin précède la date de début.");
        }

        PorteeDeleguee portee = PorteeDeleguee.builder()
                .utilisateur(beneficiaire).departement(departement).typeAcces(req.getTypeAcces())
                .dateDebut(req.getDateDebut()).dateFin(req.getDateFin())
                .accordePar(actor.getUtilisateur()).build();

        return toDTO(porteeDelegueeRepository.save(portee));
    }

    @Transactional
    public void revoquer(Long idPortee, UserDetailsImpl actor) {
        PorteeDeleguee portee = porteeDelegueeRepository.findById(idPortee)
                .orElseThrow(() -> new BusinessException("PORTEE_INTROUVABLE", "Délégation introuvable : " + idPortee));

        boolean estAccordant = portee.getAccordePar().getId().equals(actor.getId());
        VisibilityScope scopeActor = visibiliteService.resoudreScope(actor);
        boolean aAutoriteSurDepartement = scopeActor.autoriseEcriture(null, portee.getDepartement().getChemin());

        if (!estAccordant && !aAutoriteSurDepartement) {
            throw new AccessDeniedException("Vous n'avez pas l'autorité pour révoquer cette délégation.");
        }

        Long idBeneficiaire = portee.getUtilisateur().getId();

        LocalDate aujourdhui = LocalDate.now();
        if (portee.getDateDebut().isAfter(aujourdhui)) {
            porteeDelegueeRepository.delete(portee);
        } else {
            portee.setDateFin(aujourdhui.minusDays(1));
            porteeDelegueeRepository.save(portee);
        }


        cacheManager.getCache("visibilityScope").evict(idBeneficiaire);
    }

    /** Révocation = positionner dateFin (traçabilité) ; suppression pure seulement si jamais démarrée. */
/*
    @Transactional
    @CacheEvict(value = "visibilityScope", key = "#idUtilisateurBeneficiaire")
    public void revoquer(Long idPortee, Long idUtilisateurBeneficiaire, UserDetailsImpl actor) {
        PorteeDeleguee portee = porteeDelegueeRepository.findById(idPortee)
                .orElseThrow(() -> new BusinessException("PORTEE_INTROUVABLE", "Délégation introuvable : " + idPortee));

        boolean estAccordant = portee.getAccordePar().getId().equals(actor.getId());
        VisibilityScope scopeActor = visibiliteService.resoudreScope(actor);
        boolean aAutoriteSurDepartement = scopeActor.autoriseEcriture(null, portee.getDepartement().getChemin());

        if (!estAccordant && !aAutoriteSurDepartement) {
            throw new AccessDeniedException("Vous n'avez pas l'autorité pour révoquer cette délégation.");
        }

        LocalDate aujourdhui = LocalDate.now();
        if (portee.getDateDebut().isAfter(aujourdhui)) {
            porteeDelegueeRepository.delete(portee);
        } else {
            portee.setDateFin(aujourdhui);
            porteeDelegueeRepository.save(portee);
        }
    }
*/

    /*@Transactional(readOnly = true)
    public List<PorteeDelegueeDTO> findPourUtilisateur(Long idUtilisateur) {
        return porteeDelegueeRepository.findByUtilisateurIdOrderByDateDebutDesc(idUtilisateur)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }*/

    @Transactional(readOnly = true)
    public List<PorteeDelegueeDTO> findPourUtilisateur(Long idUtilisateur, UserDetailsImpl actor) {
        boolean estSoiMeme = actor.getId().equals(idUtilisateur);
        if (!estSoiMeme && !actor.hasRole("ADMIN")) {
            throw new AccessDeniedException(
                    "Vous ne pouvez consulter que vos propres délégations.");
        }
        return porteeDelegueeRepository.findByUtilisateurIdOrderByDateDebutDesc(idUtilisateur)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private PorteeDelegueeDTO toDTO(PorteeDeleguee p) {
        return new PorteeDelegueeDTO(
                p.getId(), p.getUtilisateur().getId(), p.getUtilisateur().getEmail(),
                p.getDepartement().getId(), p.getDepartement().getNomDepartement(),
                p.getTypeAcces(), p.getDateDebut(), p.getDateFin(),
                p.getAccordePar().getEmail(), p.estActive(LocalDate.now()));
    }
}