package com.mechanosaur.game;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Система зубчатых передач: сцепление, направление вращения, передаточные числа.
 */
public final class GearSystem {

    private final List<Gear> gears = new ArrayList<>();

    /** Ставит шестерню; false — если клетка занята или наложение. */
    public boolean place(Gear gear) {
        for (Gear g : gears) {
            if (g.x == gear.x && g.y == gear.y) return false;
            if (g.overlaps(gear)) return false;
        }
        gears.add(gear);
        return true;
    }

    public int size() {
        return gears.size();
    }

    public Gear gearAt(int index) {
        return gears.get(index);
    }

    public boolean areMeshed(int i, int j) {
        return gears.get(i).meshesWith(gears.get(j));
    }

    /**
     * Направление вращения относительно шестерни 0 (+1 / -1), 0 — вне цепи.
     * Каждое сцепление меняет направление.
     */
    public int rotationOf(int index) {
        if (index < 0 || index >= gears.size() || gears.isEmpty()) return 0;
        Map<Integer, Integer> signs = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        signs.put(0, 1);
        queue.add(0);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            for (int i = 0; i < gears.size(); i++) {
                if (i == cur || signs.containsKey(i)) continue;
                if (areMeshed(cur, i)) {
                    signs.put(i, -signs.get(cur));
                    queue.add(i);
                }
            }
        }
        return signs.getOrDefault(index, 0);
    }

    /** Есть ли цепь сцеплений от i до j. */
    public boolean connected(int i, int j) {
        if (i < 0 || j < 0 || i >= gears.size() || j >= gears.size()) return false;
        boolean[] seen = new boolean[gears.size()];
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        seen[i] = true;
        queue.add(i);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == j) return true;
            for (int k = 0; k < gears.size(); k++) {
                if (!seen[k] && areMeshed(cur, k)) {
                    seen[k] = true;
                    queue.add(k);
                }
            }
        }
        return false;
    }

    /** Передаточное число от i к j: произведение r_ведущая/r_ведомая вдоль цепи. */
    public double ratioFrom(int i, int j) {
        if (!connected(i, j)) return 0.0;
        // Поиск в ширину с накоплением произведения
        Map<Integer, Double> ratios = new HashMap<>();
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        ratios.put(i, 1.0);
        queue.add(i);
        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (cur == j) return ratios.get(cur);
            for (int k = 0; k < gears.size(); k++) {
                if (ratios.containsKey(k) || !areMeshed(cur, k)) continue;
                ratios.put(k, ratios.get(cur) * ((double) gears.get(cur).radius / gears.get(k).radius));
                queue.add(k);
            }
        }
        return ratios.getOrDefault(j, 0.0);
    }

    /** Обороты выходной шестерни при известных оборотах входной. */
    public double outputRpm(int inputIndex, int outputIndex, double inputRpm) {
        return inputRpm * ratioFrom(inputIndex, outputIndex);
    }
}
