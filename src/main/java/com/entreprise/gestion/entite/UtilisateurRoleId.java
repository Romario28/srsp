package com.entreprise.gestion.entite;

import java.io.Serializable;
import java.util.Objects;

/**
 * Clé composite pour l'entité UtilisateurRole.
 *
 * Requise par JPA (@IdClass) car UTILISATEUR_ROLE possède une colonne
 * supplémentaire (date_attribution) qui empêche l'utilisation d'un simple
 * @ManyToMany. La clé primaire est composite : (id_utilisateur, id_role).
 *
 * Les noms des champs doivent correspondre exactement aux noms des champs
 * @Id dans UtilisateurRole (ici : "utilisateur" et "role").
 */
public class UtilisateurRoleId implements Serializable {


    private static final long serialVersionUID = 1L;
    private Long utilisateur;
    private Long role;

    public UtilisateurRoleId() {}

    public UtilisateurRoleId(Long utilisateur, Long role) {
        this.utilisateur = utilisateur;
        this.role = role;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UtilisateurRoleId)) return false;
        UtilisateurRoleId that = (UtilisateurRoleId) o;
        return Objects.equals(utilisateur, that.utilisateur)
            && Objects.equals(role, that.role);
    }

    @Override
    public int hashCode() {
        return Objects.hash(utilisateur, role);
    }
}
