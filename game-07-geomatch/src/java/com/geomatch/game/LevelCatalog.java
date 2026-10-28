package com.geomatch.game;

/** Каталог уровней match-3: растущие цели и ограничение ходов. */
public final class LevelCatalog {

    public static final int LEVEL_COUNT = 40;

    public static final class Params {
        public final int moves;
        public final int targetScore;
        public final int targetColor;
        public final int targetCount;

        Params(int moves, int targetScore, int targetColor, int targetCount) {
            this.moves = moves;
            this.targetScore = targetScore;
            this.targetColor = targetColor;
            this.targetCount = targetCount;
        }
    }

    private LevelCatalog() {
    }

    public static Params paramsFor(int level) {
        if (level < 1 || level > LEVEL_COUNT) throw new IllegalArgumentException("Уровень вне диапазона");
        int moves = Math.max(15, 30 - level / 3);
        int targetScore = 800 + level * 350;
        int targetColor = level % MatchBoard.COLORS;
        int targetCount = 10 + level;
        return new Params(moves, targetScore, targetColor, targetCount);
    }
}
