package com.entreprise.gestion.service.dossier;

/** Nombre total de dossiers d'un agent, tous états confondus. */
public record NbDossiersAgent(String matricule, Long nombre) {}
