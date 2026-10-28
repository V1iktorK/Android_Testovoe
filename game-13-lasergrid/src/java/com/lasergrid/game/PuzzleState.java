package com.lasergrid.game;

import java.util.List;

/** Состояние головоломки: текущая раскладка, эталон, счётчик ходов. */
public final class PuzzleState {

    private final char[][] grid;
    private final char[][] reference;
    private final List<int[]> mirrors;
    private int moves;

    PuzzleState(char[][] grid, char[][] reference, List<int[]> mirrors) {
        this.grid = grid;
        this.reference = reference;
        this.mirrors = mirrors;
    }

    public char[][] grid() {
        return grid;
    }

    public int mirrorCount() {
        return mirrors.size();
    }

    public int moveCount() {
        return moves;
    }

    public boolean isSolved() {
        return BeamLogic.allTargetsLit(grid);
    }

    /** Решаем ли уровень в эталонной раскладке (проверка генератора). */
    public boolean isSolvedInReferenceState() {
        return BeamLogic.allTargetsLit(reference);
    }

    /** Поворот зеркала: '/' <-> '\'. */
    public void rotate(int mirrorIndex) {
        int[] m = mirrors.get(mirrorIndex);
        grid[m[1]][m[0]] = grid[m[1]][m[0]] == '/' ? '\\' : '/';
        moves++;
    }

    /** Поворот зеркала в клетке сетки; false — если в клетке нет зеркала. */
    public boolean rotateAt(int x, int y) {
        for (int[] m : mirrors) {
            if (m[0] == x && m[1] == y) {
                grid[y][x] = grid[y][x] == '/' ? '\\' : '/';
                moves++;
                return true;
            }
        }
        return false;
    }

    /** Первое зеркало, отличающееся от эталона, или -1. */
    public int firstUnsolvedMirror() {
        for (int i = 0; i < mirrors.size(); i++) {
            int[] m = mirrors.get(i);
            if (grid[m[1]][m[0]] != reference[m[1]][m[0]]) return i;
        }
        return -1;
    }

    /** Число поворотов до решения (par): зеркала, отличные от эталона. */
    public int parMoves() {
        int total = 0;
        for (int[] m : mirrors) {
            if (grid[m[1]][m[0]] != reference[m[1]][m[0]]) total++;
        }
        return total;
    }

    public String signature() {
        StringBuilder sb = new StringBuilder();
        for (char[] row : grid) sb.append(row).append('\n');
        sb.append("moves=").append(moves);
        return sb.toString();
    }

    /** Звёзды: 3 — ровно par ходов, 2 — до 1.5×par, 1 — иначе. */
    public static int starsFor(int moves, int par) {
        if (moves <= par) return 3;
        if (moves <= par * 3 / 2) return 2;
        return 1;
    }
}
