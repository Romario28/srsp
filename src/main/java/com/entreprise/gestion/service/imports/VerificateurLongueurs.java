package com.entreprise.gestion.service.imports;

import com.entreprise.gestion.exception.BusinessException;
import jakarta.persistence.Column;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Vérifie les longueurs déclarées sur les colonnes avant qu'un flush ne puisse annuler tout l'import. */
@Component
@RequiredArgsConstructor
public class VerificateurLongueurs {

    private record ChampLimite(Field champ, String colonne, int max) {}
    private static final Map<Class<?>, List<ChampLimite>> CACHE = new ConcurrentHashMap<>();
    private final EntityManager entityManager;

    public void verifier(Object entite) {
        List<String> problemes = new ArrayList<>();
        for (ChampLimite c : limites(Hibernate.getClass(entite))) {
            try {
                if (c.champ().get(entite) instanceof String valeur && valeur.length() > c.max()) {
                    problemes.add("« " + c.colonne() + " » trop long (" + valeur.length()
                            + " caractères, maximum " + c.max() + ") : « " + abreger(valeur) + " »");
                }
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
        if (problemes.isEmpty()) return;
        if (entityManager.contains(entite)) entityManager.detach(entite);
        throw new BusinessException("VALEUR_TROP_LONGUE", String.join(" ; ", problemes));
    }

    private static List<ChampLimite> limites(Class<?> type) {
        return CACHE.computeIfAbsent(type, t -> {
            List<ChampLimite> liste = new ArrayList<>();
            for (Field f : t.getDeclaredFields()) {
                Column col = f.getAnnotation(Column.class);
                if (col == null || f.getType() != String.class || !col.columnDefinition().isEmpty()) continue;
                f.setAccessible(true);
                liste.add(new ChampLimite(f, col.name().isEmpty() ? f.getName() : col.name(), col.length()));
            }
            return List.copyOf(liste);
        });
    }

    private static String abreger(String s) {
        return s.length() <= 30 ? s : s.substring(0, 30) + "…";
    }
}
