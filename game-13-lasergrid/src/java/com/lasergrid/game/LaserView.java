package com.lasergrid.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.List;

/**
 * «Луч-88» — головоломка освещения пути: поверни зеркала, направь луч на все цели.
 * Векторная графика 80-х: чёрный фон, светящиеся линии «осциллографа».
 */
public final class LaserView extends View {

    private enum State { Menu, LevelSelect, Play, Win }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private State state = State.Menu;
    private PuzzleState puzzle;
    private int currentLevel = 1;
    private int par = 0;
    private final java.util.Map<String, RectF> buttons = new java.util.LinkedHashMap<>();
    private final ProgressStore progress;

    // Палитра «зелёный фосфор» + циан
    private final int bg = Color.rgb(6, 10, 8);
    private final int beam = Color.rgb(60, 255, 120);
    private final int beamDim = Color.rgb(24, 90, 48);
    private final int mirrorColor = Color.rgb(80, 220, 255);
    private final int targetColor = Color.rgb(255, 90, 90);
    private final int textColor = Color.rgb(140, 255, 170);

    public LaserView(Context context) {
        super(context);
        progress = new ProgressStore(context);
    }

    private void startLevel(int level) {
        currentLevel = level;
        int difficulty = 1 + (level - 1) / 8;
        puzzle = LevelGenerator.generate(20261028L + level * 31, difficulty);
        par = puzzle.parMoves();
        state = State.Play;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(bg);
        // люминофор осциллографа: горизонтальные строки развёртки
        paint.setColor(Color.argb(30, 0, 0, 0));
        for (int sy = 0; sy < getHeight(); sy += 6) canvas.drawRect(0, sy, getWidth(), sy + 2, paint);
        buttons.clear();
        switch (state) {
            case Menu:
                drawMenu(canvas);
                break;
            case LevelSelect:
                drawLevelSelect(canvas);
                break;
            case Play:
            case Win:
                drawPuzzle(canvas);
                if (state == State.Win) drawWin(canvas);
                break;
        }
    }

    private void drawPuzzle(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        char[][] grid = puzzle.grid();
        int rows = grid.length;
        int cols = grid[0].length;
        float cell = Math.min(w * 0.92f / cols, h * 0.62f / rows);
        float left = (w - cols * cell) / 2f;
        float top = h * 0.22f;

        boolean[][] lit = BeamLogic.trace(grid);

        // Сетка-«осциллограф»
        paint.setColor(Color.rgb(16, 34, 22));
        paint.setStrokeWidth(2f);
        for (int x = 0; x <= cols; x++) canvas.drawLine(left + x * cell, top, left + x * cell, top + rows * cell, paint);
        for (int y = 0; y <= rows; y++) canvas.drawLine(left, top + y * cell, left + cols * cell, top + y * cell, paint);

        // Освещённые клетки — свечение луча
        paint.setColor(beamDim);
        for (int y = 0; y < rows; y++)
            for (int x = 0; x < cols; x++)
                if (lit[y][x] && grid[y][x] != '#')
                    canvas.drawRect(left + x * cell + 4, top + y * cell + 4,
                            left + (x + 1) * cell - 4, top + (y + 1) * cell - 4, paint);

        // Клетки: зеркала, цели, источник
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                float cx = left + (x + 0.5f) * cell;
                float cy = top + (y + 0.5f) * cell;
                char c = grid[y][x];
                if (c == '/' || c == '\\') {
                    paint.setColor(mirrorColor);
                    paint.setStrokeWidth(cell * 0.12f);
                    paint.setStrokeCap(Paint.Cap.ROUND);
                    if (c == '/') canvas.drawLine(cx - cell * 0.32f, cy + cell * 0.32f, cx + cell * 0.32f, cy - cell * 0.32f, paint);
                    else canvas.drawLine(cx - cell * 0.32f, cy - cell * 0.32f, cx + cell * 0.32f, cy + cell * 0.32f, paint);
                } else if (c == 'T') {
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(cell * 0.1f);
                    paint.setColor(lit[y][x] ? beam : targetColor);
                    canvas.drawCircle(cx, cy, cell * 0.3f, paint);
                    paint.setStyle(Paint.Style.FILL);
                } else if (c == 'S') {
                    paint.setColor(beam);
                    canvas.drawCircle(cx, cy, cell * 0.24f, paint);
                }
            }
        }

        // HUD
        paint.setColor(textColor);
        paint.setTextSize(w * 0.05f);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Уровень " + currentLevel, w * 0.04f, w * 0.1f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("ходы: " + puzzle.moveCount() + "  par: " + par, w * 0.96f, w * 0.1f, paint);
        button(canvas, "menu", "Меню", w * 0.5f, h * 0.94f, w * 0.4f, h * 0.055f);
    }

    private void drawWin(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(Color.argb(190, 6, 10, 8));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(beam);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.09f);
        canvas.drawText("ЛУЧ НАПРАВЛЕН!", w / 2f, h * 0.42f, paint);
        paint.setTextSize(w * 0.05f);
        paint.setColor(textColor);
        canvas.drawText("★ " + PuzzleState.starsFor(puzzle.moveCount(), par)
                + "   ходов: " + puzzle.moveCount(), w / 2f, h * 0.48f, paint);
        button(canvas, "next", "Дальше", w / 2f, h * 0.58f, w * 0.5f, h * 0.07f);
        button(canvas, "menu", "К уровням", w / 2f, h * 0.68f, w * 0.5f, h * 0.07f);
    }

    private void drawMenu(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(beam);
        paint.setTextSize(w * 0.13f);
        canvas.drawText("ЛУЧ-88", w / 2f, h * 0.3f, paint);
        paint.setColor(textColor);
        paint.setTextSize(w * 0.042f);
        canvas.drawText("векторная головоломка: поверни зеркала", w / 2f, h * 0.36f, paint);
        canvas.drawText("и направь луч на все цели", w / 2f, h * 0.41f, paint);
        button(canvas, "play", "Играть", w / 2f, h * 0.55f, w * 0.55f, h * 0.075f);
        paint.setTextSize(w * 0.038f);
        paint.setColor(beamDim);
        canvas.drawText("40 уровней · всего звёзд: " + progress.totalStars(), w / 2f, h * 0.66f, paint);
    }

    private void drawLevelSelect(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(beam);
        paint.setTextSize(w * 0.08f);
        canvas.drawText("Уровни", w / 2f, h * 0.08f, paint);
        for (int i = 1; i <= 40; i++) {
            int col = (i - 1) % 5;
            int row = (i - 1) / 5;
            float cw = w * 0.17f;
            float x = w * 0.05f + col * w * 0.19f;
            float y = h * 0.12f + row * h * 0.088f;
            RectF r = new RectF(x, y, x + cw, y + h * 0.07f);
            buttons.put("lvl" + i, r);
            boolean unlocked = progress.isUnlocked(i);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(4);
            paint.setColor(unlocked ? beam : beamDim);
            canvas.drawRoundRect(r, 18, 18, paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(unlocked ? beam : beamDim);
            paint.setTextSize(cw * 0.32f);
            canvas.drawText(String.valueOf(i), r.centerX(), r.centerY() + cw * 0.12f, paint);
        }
        button(canvas, "menu", "Назад", w / 2f, h * 0.95f, w * 0.45f, h * 0.05f);
    }

    private void button(Canvas canvas, String name, String label, float cx, float cy, float bw, float bh) {
        RectF r = new RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2);
        buttons.put(name, r);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setColor(beam);
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bh * 0.42f);
        canvas.drawText(label, cx, cy + bh * 0.15f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) return true;
        float x = event.getX();
        float y = event.getY();

        for (java.util.Map.Entry<String, RectF> e : buttons.entrySet()) {
            if (!e.getValue().contains(x, y)) continue;
            String name = e.getKey();
            if (name.startsWith("lvl")) {
                int lvl = Integer.parseInt(name.substring(3));
                if (progress.isUnlocked(lvl)) startLevel(lvl);
            } else if (name.equals("play")) {
                state = State.LevelSelect;
            } else if (name.equals("menu")) {
                if (state == State.Win) state = State.LevelSelect;
                else state = State.Menu;
            } else if (name.equals("next")) {
                if (currentLevel < 40) startLevel(currentLevel + 1);
                else state = State.LevelSelect;
            }
            invalidate();
            return true;
        }

        if (state == State.Play) {
            float w = getWidth();
            float h = getHeight();
            char[][] grid = puzzle.grid();
            int rows = grid.length;
            int cols = grid[0].length;
            float cell = Math.min(w * 0.92f / cols, h * 0.62f / rows);
            float left = (w - cols * cell) / 2f;
            float top = h * 0.22f;
            int bx = (int) ((x - left) / cell);
            int by = (int) ((y - top) / cell);
            if (bx >= 0 && bx < cols && by >= 0 && by < rows) {
                rotateMirrorAt(bx, by);
                if (puzzle.isSolved()) {
                    progress.complete(currentLevel, PuzzleState.starsFor(puzzle.moveCount(), par));
                    state = State.Win;
                }
                invalidate();
            }
        }
        return true;
    }

    private void rotateMirrorAt(int x, int y) {
        puzzle.rotateAt(x, y);
    }

    /** Сохранение прогресса (уровни и звёзды). */
    private static final class ProgressStore {
        private final android.content.SharedPreferences prefs;

        ProgressStore(Context context) {
            prefs = context.getSharedPreferences("lasergrid", Context.MODE_PRIVATE);
        }

        void complete(int level, int stars) {
            if (stars > starsFor(level)) prefs.edit().putInt("stars" + level, stars).apply();
        }

        int starsFor(int level) {
            return prefs.getInt("stars" + level, 0);
        }

        boolean isUnlocked(int level) {
            return level == 1 || starsFor(level - 1) > 0;
        }

        int totalStars() {
            int total = 0;
            for (int i = 1; i <= 40; i++) total += starsFor(i);
            return total;
        }
    }
}
