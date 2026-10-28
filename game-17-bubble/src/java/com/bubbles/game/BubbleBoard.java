package com.bubbles.game;

import java.util.Random;

/**
 * Поле пузырьков: генератор с цветовым балансом (ось C), выстрелы, очистка цепочек.
 */
public final class BubbleBoard {

    public static final int COLORS = 5;
    public static final int WIDTH = 8;

    private final int[][] grid;

    public BubbleBoard(int[][] grid) {
        this.grid = grid;
    }

    /** Детерминированное поле: rows заполненных рядов, цвета сбалансированы. */
    public static BubbleBoard generate(long seed, int rows) {
        Random rng = new Random(seed * 7919 + rows * 104729);
        int height = Math.max(3, rows);
        int[][] grid = new int[height][WIDTH];
        for (int y = 0; y < height; y++)
            for (int x = 0; x < WIDTH; x++)
                grid[y][x] = -1;
        // Цвета распределяются по кругу — баланс гарантирован
        int[] palette = new int[WIDTH * height];
        for (int i = 0; i < palette.length; i++) palette[i] = i % COLORS;
        for (int i = palette.length - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = palette[i];
            palette[i] = palette[j];
            palette[j] = tmp;
        }
        int k = 0;
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < WIDTH; x++)
                grid[y][x] = palette[k++];
        return new BubbleBoard(grid);
    }

    public int[][] grid() {
        return grid;
    }

    public int rows() {
        return grid.length;
    }

    public boolean isCleared() {
        for (int[] row : grid)
            for (int c : row)
                if (c >= 0) return false;
        return true;
    }

    /** Выстрел: пузырёк встаёт в первый свободный ряд сверху колонки. */
    public void shoot(int col, int color) {
        for (int y = 0; y < grid.length; y++) {
            if (grid[y][col] < 0) {
                grid[y][col] = color;
                return;
            }
        }
    }

    /** Удаляет цепочку, если она от 3, и роняет незакреплённые. Возвращает число очищенных. */
    public int resolveAt(int x, int y) {
        int matched = BubbleLogic.matchSize(grid, x, y);
        if (matched < 3) return 0;
        int color = grid[y][x];
        clearColor(color);
        int fallen = BubbleLogic.countFloating(grid);
        removeFloating();
        return matched + fallen;
    }

    private void clearColor(int color) {
        for (int y = 0; y < grid.length; y++)
            for (int x = 0; x < WIDTH; x++)
                if (grid[y][x] == color) grid[y][x] = -1;
    }

    private void removeFloating() {
        // Помечаем закреплённые и удаляем остальные
        boolean[][] keep = new boolean[grid.length][WIDTH];
        java.util.ArrayDeque<int[]> queue = new java.util.ArrayDeque<>();
        for (int x = 0; x < WIDTH; x++) {
            if (grid[0][x] >= 0) {
                keep[0][x] = true;
                queue.add(new int[]{x, 0});
            }
        }
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = cell[0] + d[0];
                int ny = cell[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= WIDTH || ny >= grid.length) continue;
                if (keep[ny][nx] || grid[ny][nx] < 0) continue;
                keep[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        for (int y = 0; y < grid.length; y++)
            for (int x = 0; x < WIDTH; x++)
                if (!keep[y][x]) grid[y][x] = -1;
    }

    public String signature() {
        StringBuilder sb = new StringBuilder();
        for (int[] row : grid) {
            for (int c : row) sb.append(c).append(',');
            sb.append(';');
        }
        return sb.toString();
    }
}
