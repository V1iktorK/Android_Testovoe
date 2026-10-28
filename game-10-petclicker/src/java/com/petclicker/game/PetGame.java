package com.petclicker.game;

/**
 * Игровое состояние «Пушистого кликера»: монеты, комбо-цепочки, питомцы, ивенты, сохранение.
 */
public final class PetGame {

    public static final int PET_COUNT = 8;
    private static final double[] PET_BASE_COST = {15, 120, 900, 6500, 45000, 320000, 2.2e6, 1.6e7};
    private static final double[] PET_BASE_RATE = {0.5, 3, 18, 110, 700, 4500, 30000, 220000};
    private static final double COST_GROWTH = 1.15;
    private static final double COMBO_TIMEOUT = 1.5;

    private double coins;
    private double totalTaps;
    private int combo;
    private double comboTimer;
    private final int[] petCounts = new int[PET_COUNT];

    public double coins() {
        return coins;
    }

    public int combo() {
        return combo;
    }

    public int petCount(int index) {
        return petCounts[index];
    }

    public void addCoins(double amount) {
        coins += amount;
    }

    /** Копирует состояние другого инстанса (загрузка сохранения). */
    public void restoreFrom(PetGame other) {
        this.coins = other.coins;
        this.totalTaps = other.totalTaps;
        this.combo = other.combo;
        this.comboTimer = other.comboTimer;
        System.arraycopy(other.petCounts, 0, this.petCounts, 0, this.petCounts.length);
    }

    /** Награда за тап с учётом длины комбо-цепочки. */
    public double tapValue(int comboLength) {
        return 1.0 + comboLength * 0.25;
    }

    public void tap() {
        coins += tapValue(combo);
        combo++;
        comboTimer = COMBO_TIMEOUT;
        totalTaps++;
    }

    /** Время: производство питомцев и таймаут комбо. */
    public void tick(double seconds) {
        coins += coinsPerSecond() * seconds;
        comboTimer -= seconds;
        if (comboTimer <= 0) {
            combo = 0;
            comboTimer = 0;
        }
    }

    public double coinsPerSecond() {
        double sum = 0;
        for (int i = 0; i < PET_COUNT; i++) sum += petCounts[i] * PET_BASE_RATE[i];
        return sum;
    }

    public double petCost(int index) {
        return PET_BASE_COST[index] * Math.pow(COST_GROWTH, petCounts[index]);
    }

    public boolean buyPet(int index) {
        double cost = petCost(index);
        if (coins < cost) return false;
        coins -= cost;
        petCounts[index]++;
        return true;
    }

    /** Ивенты: золотая лапка и др. */
    public void triggerEvent(EventSpawner.Type type) {
        switch (type) {
            case GOLDEN_PAW:
                coins += 100 + coins * 0.1;
                break;
            case RAINBOW_BALL:
                coins += 50 + tapValue(combo) * 25;
                break;
            case NAP_TIME:
                comboTimer = Math.max(comboTimer, 3.0);
                break;
            case TREAT_SHOWER:
                coins += 250;
                break;
        }
    }

    /** Уровень игрока: растёт от общего числа купленных питомцев. */
    public int level() {
        int total = 0;
        for (int c : petCounts) total += c;
        return 1 + total / 2;
    }

    /** Питомец index открывается на уровне 1 + index*2. */
    public static boolean isPetUnlocked(int index, int level) {
        return level >= 1 + index * 2;
    }

    // --- Сохранение: coins|totalTaps|petCounts --- 

    public String save() {
        StringBuilder sb = new StringBuilder();
        sb.append(coins).append('|').append(totalTaps).append('|');
        for (int i = 0; i < PET_COUNT; i++) {
            if (i > 0) sb.append(',');
            sb.append(petCounts[i]);
        }
        return sb.toString();
    }

    public static PetGame load(String text) {
        try {
            String[] parts = text.split("\\|");
            PetGame g = new PetGame();
            g.coins = Double.parseDouble(parts[0]);
            g.totalTaps = Double.parseDouble(parts[1]);
            String[] pets = parts[2].split(",");
            for (int i = 0; i < pets.length && i < PET_COUNT; i++) {
                g.petCounts[i] = Integer.parseInt(pets[i]);
            }
            return g;
        } catch (Exception e) {
            return new PetGame();
        }
    }
}
