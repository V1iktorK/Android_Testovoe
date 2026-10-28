package com.farm.game;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Каталог культур фермы. */
public final class CropCatalog {

    private static final Map<String, Crop> CROPS = new LinkedHashMap<>();

    static {
        add("wheat", 5, 3, 5, 1);
        add("berry", 8, 4, 8, 1);
        add("corn", 12, 6, 12, 1);
        add("carrot", 10, 5, 10, 2);
        add("pumpkin", 20, 8, 25, 2);
        add("honeydew", 24, 7, 32, 2);
        add("starfruit", 32, 10, 60, 3);
        add("moonmelon", 40, 12, 90, 3);
        add("sunblossom", 55, 15, 150, 4);
        add("crystalroot", 70, 18, 240, 4);
    }

    private static void add(String name, int growSeconds, int yield, int price, int rarity) {
        CROPS.put(name, new Crop(name, growSeconds, yield, price, rarity));
    }

    private CropCatalog() {
    }

    public static Crop get(String name) {
        return CROPS.get(name);
    }

    public static List<String> names() {
        return new ArrayList<>(CROPS.keySet());
    }
}
