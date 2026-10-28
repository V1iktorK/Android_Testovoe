package com.farm.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Книга скрещиваний (ось C: матрица наследования культур).
 * Итог детерминирован сидом и парой родителей; редкость результата не ниже редкости родителей.
 */
public final class BreedingBook {

    private BreedingBook() {
    }

    /** Скрещивание двух культур: симметрично, детерминировано, результат из каталога. */
    public static String breed(String parentA, String parentB, long seed) {
        String p1 = parentA.compareTo(parentB) <= 0 ? parentA : parentB;
        String p2 = parentA.compareTo(parentB) <= 0 ? parentB : parentA;
        Crop c1 = CropCatalog.get(p1);
        Crop c2 = CropCatalog.get(p2);
        if (c1 == null || c2 == null) return "wheat";

        int minRarity = Math.max(c1.rarity, c2.rarity);
        List<String> candidates = new ArrayList<>();
        for (String name : CropCatalog.names()) {
            if (CropCatalog.get(name).rarity >= minRarity) candidates.add(name);
        }
        Random rng = new Random(seed * 31L + p1.hashCode() * 7L + p2.hashCode());
        return candidates.get(rng.nextInt(candidates.size()));
    }

    /** Стоимость скрещивания — растёт с редкостью родителей. */
    public static int breedCost(String parentA, String parentB) {
        Crop c1 = CropCatalog.get(parentA);
        Crop c2 = CropCatalog.get(parentB);
        if (c1 == null || c2 == null) return 0;
        return 50 * (Math.max(c1.rarity, c2.rarity) + 1);
    }
}
