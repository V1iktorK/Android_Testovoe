package com.lasergrid.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Генератор уровней «solution-first» (ось C): сначала прокладывается самоизбегающий путь
 * луча с зеркалами и целями (решение гарантировано), затем зеркала на пути перемешиваются.
 */
public final class LevelGenerator {

    public static final int WIDTH = 13;
    public static final int HEIGHT = 9;

    private LevelGenerator() {
    }

    public static PuzzleState generate(long seed, int difficulty) {
        Random rng = new Random(seed * 7919 + difficulty * 104729);
        int mirrorTarget = 3 + difficulty * 2;

        for (int attempt = 0; attempt < 300; attempt++) {
            int targetsLeft = 1 + difficulty / 2;
            char[][] reference = blankGrid();
            int sy = HEIGHT / 2;
            reference[sy][1] = 'S';

            List<int[]> mirrors = new ArrayList<>();
            List<int[]> pathCells = new ArrayList<>();
            Set<String> visitedCells = new HashSet<>();
            visitedCells.add("1," + sy);

            // Самоизбегающий маршрут луча: старт строго на восток
            int x = 1, y = sy, dx = 1, dy = 0;
            boolean ok = true;
            for (int step = 0; step < 400; step++) {
                // Решаем, поворачивать ли в этой клетке
                boolean needTurn = mirrors.size() < mirrorTarget;
                boolean straightAvailable = isFree(x + dx, y + dy, visitedCells);
                int ndx = dx, ndy = dy;

                if (needTurn || !straightAvailable) {
                    int[][] turns = (dx != 0) ? new int[][]{{0, -1}, {0, 1}} : new int[][]{{1, 0}, {-1, 0}};
                    List<int[]> options = new ArrayList<>();
                    for (int[] t : turns)
                        if (isFree(x + t[0], y + t[1], visitedCells)) options.add(t);
                    if (options.isEmpty()) {
                        if (straightAvailable) {
                            // вынужденно прямо
                        } else {
                            break; // тупик — конец маршрута
                        }
                    } else {
                        int[] chosen = options.get(rng.nextInt(options.size()));
                        ndx = chosen[0];
                        ndy = chosen[1];
                        if (dx != ndx || dy != ndy) {
                            // поворот требует зеркала в текущей клетке (кроме старта)
                            if (!(x == 1 && y == sy)) {
                                reference[y][x] = mirrorFor(dx, dy, ndx, ndy);
                                mirrors.add(new int[]{x, y});
                            }
                            dx = ndx;
                            dy = ndy;
                        }
                    }
                }

                x += dx;
                y += dy;
                visitedCells.add(x + "," + y);
                pathCells.add(new int[]{x, y});
                if (x <= 0 || y <= 0 || x >= WIDTH - 1 || y >= HEIGHT - 1) break;
            }

            if (mirrors.size() < mirrorTarget) continue;

            // Цели — на свободные клетки пройденного маршрута
            Collections.shuffle(pathCells, rng);
            for (int[] cell : pathCells) {
                if (targetsLeft <= 0) break;
                if (reference[cell[1]][cell[0]] == '.') {
                    reference[cell[1]][cell[0]] = 'T';
                    targetsLeft--;
                }
            }
            if (targetsLeft > 0) continue;
            if (!BeamLogic.allTargetsLit(reference)) continue;

            // Перемешивание зеркал на пути луча
            char[][] scrambled = deepCopy(reference);
            int flipped = 0;
            for (int[] m : mirrors) {
                if (rng.nextDouble() < 0.8) {
                    scrambled[m[1]][m[0]] = reference[m[1]][m[0]] == '/' ? '\\' : '/';
                    flipped++;
                }
            }
            // Гарантия: уровень стартует нерешённым — докручиваем зеркала при необходимости
            int guard = 0;
            while (BeamLogic.allTargetsLit(scrambled) && guard < mirrors.size()) {
                int[] m = mirrors.get(guard);
                scrambled[m[1]][m[0]] = scrambled[m[1]][m[0]] == '/' ? '\\' : '/';
                flipped++;
                guard++;
            }
            if (BeamLogic.allTargetsLit(scrambled)) continue;
            return new PuzzleState(scrambled, reference, mirrors);
        }
        throw new IllegalStateException("Не удалось сгенерировать уровень");
    }

    private static char[][] blankGrid() {
        char[][] grid = new char[HEIGHT][WIDTH];
        for (int y = 0; y < HEIGHT; y++)
            for (int x = 0; x < WIDTH; x++)
                grid[y][x] = (x == 0 || y == 0 || x == WIDTH - 1 || y == HEIGHT - 1) ? '#' : '.';
        return grid;
    }

    private static boolean isFree(int x, int y, Set<String> visited) {
        return x > 0 && y > 0 && x < WIDTH - 1 && y < HEIGHT - 1 && !visited.contains(x + "," + y);
    }

    /** Зеркало, переводящее направление (dx,dy) в (ndx,ndy). */
    private static char mirrorFor(int dx, int dy, int ndx, int ndy) {
        // '/' : E->N, N->E, W->S, S->W ; '\' : E->S, S->E, N->W, W->N
        if ((dx == 1 && ndy == -1) || (dy == -1 && ndx == 1) || (dx == -1 && ndy == 1) || (dy == 1 && ndx == -1))
            return '/';
        return '\\';
    }

    private static char[][] deepCopy(char[][] src) {
        char[][] out = new char[src.length][];
        for (int i = 0; i < src.length; i++) out[i] = src[i].clone();
        return out;
    }
}
