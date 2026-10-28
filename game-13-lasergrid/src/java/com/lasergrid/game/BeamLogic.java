package com.lasergrid.game;

/**
 * Трассировка лазерного луча: отражения от зеркал '/' и '\', стены блокируют.
 * Луч выходит из 'S' вправо, проходит сквозь цели 'T', освещая их.
 */
public final class BeamLogic {

    private BeamLogic() {
    }

    public static boolean[][] trace(char[][] grid) {
        int h = grid.length;
        int w = grid[0].length;
        boolean[][] lit = new boolean[h][w];
        int sx = -1, sy = -1;
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                if (grid[y][x] == 'S') {
                    sx = x;
                    sy = y;
                }
        if (sx < 0) return lit;

        int x = sx, y = sy, dx = 1, dy = 0;
        boolean[][] visited = new boolean[h][w];
        while (true) {
            if (x < 0 || y < 0 || x >= w || y >= h) break;
            if (grid[y][x] == '#') break;
            if (visited[y][x] && grid[y][x] != 'S' && grid[y][x] != 'T' && grid[y][x] != '.') break;
            lit[y][x] = true;
            if (grid[y][x] == '/' || grid[y][x] == '\\') {
                if (visited[y][x]) break; // петля на зеркале
                visited[y][x] = true;
                int ndx = dx, ndy = dy;
                if (grid[y][x] == '/') {
                    ndx = -dy;
                    ndy = -dx;
                } else {
                    ndx = dy;
                    ndy = dx;
                }
                dx = ndx;
                dy = ndy;
            }
            x += dx;
            y += dy;
        }
        return lit;
    }

    /** Все ли цели 'T' освещены. */
    public static boolean allTargetsLit(char[][] grid) {
        boolean[][] lit = trace(grid);
        for (int y = 0; y < grid.length; y++)
            for (int x = 0; x < grid[0].length; x++)
                if (grid[y][x] == 'T' && !lit[y][x]) return false;
        return true;
    }
}
