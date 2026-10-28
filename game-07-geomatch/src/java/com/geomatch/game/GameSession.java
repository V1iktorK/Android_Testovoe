package com.geomatch.game;

/** Игровая сессия уровня: счёт, ограничение ходов, цель по цвету, звёзды. */
public final class GameSession {

    private int moves;
    private int score;
    private final int targetScore;
    private final int targetColor;
    private int collected;

    public GameSession(int moves, int targetScore, int targetColor) {
        this.moves = moves;
        this.targetScore = targetScore;
        this.targetColor = targetColor;
    }

    public void useMove() {
        if (moves > 0) moves--;
    }

    public int movesLeft() {
        return moves;
    }

    public boolean hasMovesLeft() {
        return moves > 0;
    }

    public void addScore(int points) {
        score += points;
    }

    public int score() {
        return score;
    }

    public void collectColor(int color, int count) {
        if (color == targetColor) collected += count;
    }

    public int collected() {
        return collected;
    }

    /** Цель достигнута: счёт и сбор целевого цвета. */
    public boolean isObjectiveMet(int targetCount) {
        return score >= targetScore && collected >= targetCount;
    }

    /** Звёзды по отношению счёта к цели. */
    public static int starsFor(int score, int targetScore) {
        if (targetScore <= 0) return 3;
        double ratio = (double) score / targetScore;
        if (ratio >= 1.0) return 3;
        if (ratio >= 0.7) return 2;
        return 1;
    }
}
