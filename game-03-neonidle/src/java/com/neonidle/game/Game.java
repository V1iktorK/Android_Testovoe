package com.neonidle.game;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Игровое состояние idle-кликера «Неоновое ядро».
 * Чистая логика без android.*: вся математика, престиж, офлайн-прогресс, сохранение.
 */
public final class Game {

    // --- Генераторы (кривая баланса — ось C уникальности) ---
    private static final GeneratorDef[] GENERATORS = {
        new GeneratorDef("Светодиод", 10, 1.15, 0.1),
        new GeneratorDef("Катушка", 120, 1.15, 1),
        new GeneratorDef("Трансформатор", 1_400, 1.16, 9),
        new GeneratorDef("Плазменный контур", 18_000, 1.17, 72),
        new GeneratorDef("Квантовый якорь", 240_000, 1.18, 640),
        new GeneratorDef("Неон-реактор", 3_200_000, 1.19, 6_000)
    };

    public static GeneratorDef[] generators() {
        return GENERATORS;
    }

    // --- Апгрейды: 0 — клик, далее множители генераторов и глобальные ---
    private static final double[] UPGRADE_COSTS = {
        500, 5_000, 50_000, 500_000, 5_000_000,
        60_000_000, 800_000_000, 12_000_000_000L, 200_000_000_000L, 4_000_000_000_000L
    };
    private static final String[] UPGRADE_NAMES = {
        "Криокомпрессор клика", "Сверхпроводник", "Импульсный блок", "Резонатор",
        "Плазма-фокус", "Нейросинхронизация", "Фотонный затвор", "Тёмная материя",
        "Сингулярное ядро", "Неоновый сингулярит"
    };

    public static double upgradeCost(int id) {
        return UPGRADE_COSTS[id];
    }

    public static String upgradeName(int id) {
        return UPGRADE_NAMES[id];
    }

    public static int upgradeCount() {
        return UPGRADE_COSTS.length;
    }

    // --- Состояние ---
    private double energy;
    private double totalEarned;
    private long shards;
    private int prestigeCount;
    private final int[] generatorCounts = new int[GENERATORS.length];
    private final Set<Integer> upgrades = new LinkedHashSet<>();
    private final Set<String> achievements = new LinkedHashSet<>();

    public Game() {
    }

    /** Копирует состояние другого инстанса (используется при загрузке сохранения). */
    public void restoreFrom(Game other) {
        this.energy = other.energy;
        this.totalEarned = other.totalEarned;
        this.shards = other.shards;
        this.prestigeCount = other.prestigeCount;
        System.arraycopy(other.generatorCounts, 0, this.generatorCounts, 0, this.generatorCounts.length);
        this.upgrades.clear();
        this.upgrades.addAll(other.upgrades);
        this.achievements.clear();
        this.achievements.addAll(other.achievements);
    }

    // --- Базовые операции ---

    public double energy() {
        return energy;
    }

    public double totalEarned() {
        return totalEarned;
    }

    public void addEnergy(double amount) {
        energy += amount;
        totalEarned += amount;
    }

    public int generatorCount(int index) {
        return generatorCounts[index];
    }

    public Set<String> achievements() {
        return achievements;
    }

    public int prestigeCount() {
        return prestigeCount;
    }

    public long shards() {
        return shards;
    }

    /** Глобальный множитель от осколков престижа. */
    public double shardMultiplier() {
        return 1.0 + shards * 0.05;
    }

    /** Производство энергии в секунду. */
    public double energyPerSecond() {
        double sum = 0;
        for (int i = 0; i < GENERATORS.length; i++) {
            double mult = upgrades.contains(i + 1) ? 2.0 : 1.0;
            sum += generatorCounts[i] * GENERATORS[i].production() * mult;
        }
        return sum * shardMultiplier();
    }

    /** Сила одного клика. */
    public double clickValue() {
        double base = upgrades.contains(0) ? 2.0 : 1.0;
        return base * shardMultiplier() + energyPerSecond() * 0.01;
    }

    public void click() {
        addEnergy(clickValue());
    }

    public void tick(double seconds) {
        addEnergy(energyPerSecond() * seconds);
    }

    public boolean buyGenerator(int index) {
        double cost = GENERATORS[index].costFor(generatorCounts[index]);
        if (energy < cost) {
            return false;
        }
        energy -= cost;
        generatorCounts[index]++;
        return true;
    }

    public boolean buyUpgrade(int id) {
        if (upgrades.contains(id) || energy < UPGRADE_COSTS[id]) {
            return false;
        }
        energy -= UPGRADE_COSTS[id];
        upgrades.add(id);
        return true;
    }

    public boolean hasUpgrade(int id) {
        return upgrades.contains(id);
    }

    // --- Престиж ---

    /** Осколки, начисляемые за суммарный заработок: растёт как корень квадратный. */
    public static long shardsFor(double totalEarned) {
        if (totalEarned < 1e9) {
            return 0;
        }
        return (long) Math.floor(Math.sqrt(totalEarned / 1e9));
    }

    public boolean canPrestige() {
        return shardsFor(totalEarned) > 0;
    }

    public void prestige() {
        shards += shardsFor(totalEarned);
        prestigeCount++;
        energy = 0;
        totalEarned = 0;
        for (int i = 0; i < generatorCounts.length; i++) {
            generatorCounts[i] = 0;
        }
        upgrades.clear();
    }

    // --- Офлайн-прогресс (максимум 8 часов) ---

    public double offlineGain(double seconds) {
        double capped = Math.min(seconds, 8 * 3600.0);
        return energyPerSecond() * capped;
    }

    // --- Достижения ---

    public void checkAchievements() {
        if (totalEarned >= 1e6) achievements.add("first_mega");
        if (totalEarned >= 1e9) achievements.add("first_giga");
        if (totalEarned >= 1e12) achievements.add("first_tera");
        int totalGens = 0;
        for (int c : generatorCounts) totalGens += c;
        if (totalGens >= 10) achievements.add("ten_machines");
        if (totalGens >= 100) achievements.add("hundred_machines");
        if (!upgrades.isEmpty()) achievements.add("first_upgrade");
        if (upgrades.size() >= 5) achievements.add("five_upgrades");
        if (prestigeCount >= 1) achievements.add("first_prestige");
        if (prestigeCount >= 5) achievements.add("five_prestige");
        if (shards >= 100) achievements.add("hundred_shards");
    }

    // --- Форматирование ---

    public static String formatNumber(double v) {
        if (v >= 1e12) return String.format(java.util.Locale.US, "%.1fT", v / 1e12);
        if (v >= 1e9) return String.format(java.util.Locale.US, "%.1fB", v / 1e9);
        if (v >= 1e6) return String.format(java.util.Locale.US, "%.1fM", v / 1e6);
        if (v >= 1e3) return String.format(java.util.Locale.US, "%.1fK", v / 1e3);
        return String.format(java.util.Locale.US, "%.0f", v);
    }

    // --- Сохранение: energy|totalEarned|shards|prestigeCount|generators|upgrades|achievements ---

    public String save() {
        StringBuilder gens = new StringBuilder();
        for (int i = 0; i < generatorCounts.length; i++) {
            if (i > 0) gens.append(",");
            gens.append(generatorCounts[i]);
        }
        StringBuilder ups = new StringBuilder();
        for (int id : upgrades) {
            if (ups.length() > 0) ups.append(",");
            ups.append(id);
        }
        StringBuilder ach = new StringBuilder();
        for (String a : achievements) {
            if (ach.length() > 0) ach.append(",");
            ach.append(a);
        }
        return energy + "|" + totalEarned + "|" + shards + "|" + prestigeCount + "|"
                + gens + "|" + ups + "|" + ach;
    }

    public static Game load(String text) {
        try {
            String[] parts = text.split("\\|");
            if (parts.length < 6) {
                return new Game();
            }
            Game g = new Game();
            g.energy = Double.parseDouble(parts[0]);
            g.totalEarned = Double.parseDouble(parts[1]);
            g.shards = Long.parseLong(parts[2]);
            g.prestigeCount = Integer.parseInt(parts[3]);
            String[] gens = parts[4].split(",");
            for (int i = 0; i < gens.length && i < g.generatorCounts.length; i++) {
                g.generatorCounts[i] = Integer.parseInt(gens[i]);
            }
            if (!parts[5].isEmpty()) {
                for (String u : parts[5].split(",")) {
                    g.upgrades.add(Integer.parseInt(u));
                }
            }
            if (parts.length > 6 && !parts[6].isEmpty()) {
                for (String a : parts[6].split(",")) {
                    g.achievements.add(a);
                }
            }
            return g;
        } catch (Exception e) {
            return new Game();
        }
    }
}
