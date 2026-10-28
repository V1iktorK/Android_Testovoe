package com.geomatch.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Правила match-3: поиск совпадений, спец-плитки за длинные цепочки, скоринг с каскадами.
 */
public final class MatchLogic {

    public enum Special { NONE, LINE_HORIZONTAL, LINE_VERTICAL }

    private MatchLogic() {
    }

    /** Все клетки, входящие в горизонтальные/вертикальные цепочки от 3. */
    public static List<int[]> findMatches(int[][] grid) {
        int size = grid.length;
        boolean[][] marked = new boolean[size][size];
        // горизонтали
        for (int y = 0; y < size; y++) {
            int runStart = 0;
            for (int x = 1; x <= size; x++) {
                boolean same = x < size && grid[y][x] == grid[y][runStart] && grid[y][x] >= 0;
                if (!same) {
                    if (x - runStart >= 3)
                        for (int k = runStart; k < x; k++) marked[y][k] = true;
                    runStart = x;
                }
            }
        }
        // вертикали
        for (int x = 0; x < size; x++) {
            int runStart = 0;
            for (int y = 1; y <= size; y++) {
                boolean same = y < size && grid[y][x] == grid[runStart][x] && grid[y][x] >= 0;
                if (!same) {
                    if (y - runStart >= 3)
                        for (int k = runStart; k < y; k++) marked[k][x] = true;
                    runStart = y;
                }
            }
        }
        List<int[]> result = new ArrayList<>();
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++)
                if (marked[y][x]) result.add(new int[]{x, y});
        return result;
    }

    /** Очки за цепочку: 10 за клетку, множитель каскада. */
    public static int scoreFor(int cells, int cascadeLevel) {
        return cells * 10 * (cascadeLevel + 1);
    }

    /** Спец-плитка за длину цепочки: 4 — горизонтальный бластер, 5+ — вертикальный. */
    public static Special specialForRun(int runLength) {
        if (runLength >= 5) return Special.LINE_VERTICAL;
        if (runLength == 4) return Special.LINE_HORIZONTAL;
        return Special.NONE;
    }
}
