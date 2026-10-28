package com.mechanosaur.game;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

/**
 * Генератор уровней (ось C: параметрические шаблоны зубчатых передач).
 * Целевое передаточное число достигается эталонной парой шестерён; остальное — отвлекающие.
 */
public final class LevelGenerator {

    /** Уровень: эталонная сборка, инвентарь, целевое передаточное число. */
    public static final class Level {
        public final double targetRatio;
        public final double inputRpm;
        public final Map<Integer, Integer> inventory; // радиус -> количество
        public final Gear referenceIn;
        public final Gear referenceOut;
        public final int parMoves;

        Level(double targetRatio, double inputRpm, Map<Integer, Integer> inventory,
              Gear referenceIn, Gear referenceOut, int parMoves) {
            this.targetRatio = targetRatio;
            this.inputRpm = inputRpm;
            this.inventory = inventory;
            this.referenceIn = referenceIn;
            this.referenceOut = referenceOut;
            this.parMoves = parMoves;
        }

        /** Эталонная сборка: две шестерни. */
        public GearSystem buildReference() {
            GearSystem sys = new GearSystem();
            sys.place(referenceIn);
            sys.place(referenceOut);
            return sys;
        }

        public int gearCount() {
            return 2;
        }

        /** Число ходов эталонной сборки (par). */
        public int parMoves() {
            return parMoves;
        }

        /** Эталон решает уровень: выход крутится с целевым передаточным числом. */
        public boolean solvesReference() {
            GearSystem ref = buildReference();
            double rpm = ref.outputRpm(0, 1, inputRpm);
            return Math.abs(rpm / inputRpm - targetRatio) < 0.01;
        }

        /** Инвентаря хватает на эталонную сборку. */
        public boolean referenceFitsInventory() {
            Map<Integer, Integer> need = new LinkedHashMap<>();
            need.merge(referenceIn.radius, 1, Integer::sum);
            need.merge(referenceOut.radius, 1, Integer::sum);
            for (Map.Entry<Integer, Integer> e : need.entrySet()) {
                if (inventory.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
            }
            return true;
        }

        public String signature() {
            return targetRatio + "|" + inputRpm + "|" + inventory + "|"
                    + referenceIn.x + "," + referenceIn.y + "," + referenceIn.radius + "|"
                    + referenceOut.x + "," + referenceOut.y + "," + referenceOut.radius;
        }
    }

    private LevelGenerator() {
    }

    public static Level generate(long seed, int difficulty) {
        Random rng = new Random(seed * 7919 + difficulty * 104729);
        double targetRatio = 1.0 + difficulty;
        double inputRpm = 30.0;

        int bigRadius = difficulty + 1;
        Gear in = new Gear(2, 4, bigRadius);
        Gear out = new Gear(2 + bigRadius + 1, 4, 1);

        Map<Integer, Integer> inventory = new LinkedHashMap<>();
        inventory.put(bigRadius, 1);
        inventory.put(1, 1);
        inventory.put(2, 1 + rng.nextInt(2));
        inventory.put(1, inventory.get(1) + 1 + rng.nextInt(2));

        return new Level(targetRatio, inputRpm, inventory, in, out, 2);
    }
}
