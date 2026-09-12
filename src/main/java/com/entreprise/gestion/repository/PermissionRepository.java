package com.entreprise.gestion.repository;

import com.entreprise.gestion.entite.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByNomPermission(String nomPermission);
}
