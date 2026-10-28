package com.bubbles.game;

import java.util.ArrayDeque;

/** Правила пузырькового шутера: цепочки одного цвета и падение незакреплённых. */
public final class BubbleLogic {

    private BubbleLogic() {
    }

    /** Размер цепочки одного цвета вокруг клетки (включая её). */
    public static int matchSize(int[][] grid, int x, int y) {
        int rows = grid.length;
        int cols = grid[0].length;
        int color = grid[y][x];
        if (color < 0) return 0;
        boolean[][] seen = new boolean[rows][cols];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{x, y});
        seen[y][x] = true;
        int count = 0;
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            count++;
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = cell[0] + d[0];
                int ny = cell[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue;
                if (seen[ny][nx] || grid[ny][nx] != color) continue;
                seen[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        return count;
    }

    /** Число незакреплённых пузырьков (не связанных с верхним рядом). */
    public static int countFloating(int[][] grid) {
        int rows = grid.length;
        int cols = grid[0].length;
        boolean[][] attached = new boolean[rows][cols];
        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (int x = 0; x < cols; x++) {
            if (grid[0][x] >= 0) {
                attached[0][x] = true;
                queue.add(new int[]{x, 0});
            }
        }
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
            for (int[] d : dirs) {
                int nx = cell[0] + d[0];
                int ny = cell[1] + d[1];
                if (nx < 0 || ny < 0 || nx >= cols || ny >= rows) continue;
                if (attached[ny][nx] || grid[ny][nx] < 0) continue;
                attached[ny][nx] = true;
                queue.add(new int[]{nx, ny});
            }
        }
        int floating = 0;
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                if (grid[y][x] >= 0 && !attached[y][x]) floating++;
        return floating;
    }

    /** Очки за очищенную цепочку с множителем комбо. */
    public static int scoreFor(int cleared, int chain) {
        return cleared * 10 * chain;
    }
}
