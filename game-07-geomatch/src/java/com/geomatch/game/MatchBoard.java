package com.geomatch.game;

import java.util.List;
import java.util.Random;

/**
 * Доска match-3: генерация без готовых совпадений и тупиков (ось C),
 * обмен с проверкой, гравитация и дозаполнение.
 */
public final class MatchBoard {

    public static final int COLORS = 6;

    private final int size;
    private final int[][] grid;

    public MatchBoard(int size, long seed) {
        this.size = size;
        this.grid = new int[size][size];
        Random rng = new Random(seed);
        // Генерация с раскаткой совпадений
        for (int attempt = 0; attempt < 100; attempt++) {
            for (int y = 0; y < size; y++)
                for (int x = 0; x < size; x++)
                    grid[y][x] = rng.nextInt(COLORS);
            while (!MatchLogic.findMatches(grid).isEmpty()) {
                for (int[] cell : MatchLogic.findMatches(grid))
                    grid[cell[1]][cell[0]] = rng.nextInt(COLORS);
            }
            if (hasAvailableMove()) return;
            rng.setSeed(rng.nextLong());
        }
    }

    public int[][] grid() {
        return grid;
    }

    public int size() {
        return size;
    }

    public int colorAt(int x, int y) {
        return grid[y][x];
    }

    public void clearCell(int x, int y) {
        grid[y][x] = -1;
    }

    /**
     * Обмен соседних клеток. Возвращает true, если обмен создал совпадение;
     * бессмысленный обмен откатывается.
     */
    public boolean swap(int x1, int y1, int x2, int y2) {
        if (Math.abs(x1 - x2) + Math.abs(y1 - y2) != 1) return false;
        int tmp = grid[y1][x1];
        grid[y1][x1] = grid[y2][x2];
        grid[y2][x2] = tmp;
        if (MatchLogic.findMatches(grid).isEmpty()) {
            grid[y2][x2] = grid[y1][x1];
            grid[y1][x1] = tmp;
            return false;
        }
        return true;
    }

    /** Есть ли хотя бы один обмен, создающий совпадение. */
    public boolean hasAvailableMove() {
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                if (x + 1 < size && swapCreatesMatch(x, y, x + 1, y)) return true;
                if (y + 1 < size && swapCreatesMatch(x, y, x, y + 1)) return true;
            }
        }
        return false;
    }

    private boolean swapCreatesMatch(int x1, int y1, int x2, int y2) {
        int tmp = grid[y1][x1];
        grid[y1][x1] = grid[y2][x2];
        grid[y2][x2] = tmp;
        boolean creates = !MatchLogic.findMatches(grid).isEmpty();
        grid[y2][x2] = grid[y1][x1];
        grid[y1][x1] = tmp;
        return creates;
    }

    /** Гравитация: падение плиток вниз и дозаполнение сверху (детерминировано по сиду). */
    public void applyGravityAndRefill(long seed) {
        Random rng = new Random(seed);
        for (int x = 0; x < size; x++) {
            int write = size - 1;
            for (int y = size - 1; y >= 0; y--) {
                if (grid[y][x] >= 0) {
                    grid[write][x] = grid[y][x];
                    write--;
                }
            }
            for (int y = write; y >= 0; y--) {
                grid[y][x] = rng.nextInt(COLORS);
            }
        }
    }
}
