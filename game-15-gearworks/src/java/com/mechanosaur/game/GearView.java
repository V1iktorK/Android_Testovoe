package com.mechanosaur.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * «Механозавр» — пазл сборки зубчатой передачи: выложи шестерни, чтобы выход
 * вращался с целевым передаточным числом. Стимпанк: латунь, медь, шестерни.
 */
public final class GearView extends View {

    private enum State { Menu, LevelSelect, Play, Win }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private State state = State.Menu;
    private LevelGenerator.Level level;
    private GearSystem system;
    private final Map<Integer, Integer> remaining = new HashMap<>();
    private int selectedRadius = 1;
    private int currentLevel = 1;
    private int moves = 0;
    private final List<String[]> placed = new ArrayList<>(); // x,y,radius
    private final java.util.Map<String, RectF> buttons = new java.util.LinkedHashMap<>();
    private final ProgressStore progress;

    // Стимпанк-палитра
    private final int bg = Color.rgb(43, 33, 26);
    private final int brass = Color.rgb(196, 150, 74);
    private final int copper = Color.rgb(178, 106, 66);
    private final int panel = Color.rgb(62, 48, 36);
    private final int textLight = Color.rgb(242, 226, 196);
    private final int good = Color.rgb(120, 200, 120);

    public GearView(Context context) {
        super(context);
        progress = new ProgressStore(context);
    }

    private void startLevel(int levelNumber) {
        currentLevel = levelNumber;
        int difficulty = 1 + (levelNumber - 1) / 7;
        level = LevelGenerator.generate(20261028L + levelNumber * 37, difficulty);
        system = new GearSystem();
        remaining.clear();
        for (Map.Entry<Integer, Integer> e : level.inventory.entrySet())
            remaining.put(e.getKey(), e.getValue());
        selectedRadius = 1;
        moves = 0;
        placed.clear();
        state = State.Play;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(bg);
        // патентный чертёж: мелкая штриховка поля
        paint.setColor(Color.rgb(58, 45, 35));
        paint.setStrokeWidth(2f);
        for (int gx = 0; gx < getWidth(); gx += 42) canvas.drawLine(gx, 0, gx, getHeight(), paint);
        for (int gy = 0; gy < getHeight(); gy += 42) canvas.drawLine(0, gy, getWidth(), gy, paint);
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
                drawPlay(canvas);
                if (state == State.Win) drawWin(canvas);
                break;
        }
    }

    private void drawPlay(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(panel);
        canvas.drawRoundRect(new RectF(0, 0, w, h * 0.15f), 0, 0, paint);
        paint.setColor(textLight);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(w * 0.05f);
        canvas.drawText("Уровень " + currentLevel, w * 0.04f, w * 0.08f, paint);
        paint.setTextSize(w * 0.038f);
        canvas.drawText("цель: передаточное число ×" + String.format(java.util.Locale.US, "%.1f", level.targetRatio),
                w * 0.04f, w * 0.135f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("вход: " + (int) level.inputRpm + " об/мин", w * 0.96f, w * 0.08f, paint);

        // Место сборки — «стол» с сеткой
        float gridTop = h * 0.18f;
        float cell = w / 14f;
        paint.setColor(Color.rgb(52, 40, 30));
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2f);
        for (int gx = 0; gx <= 14; gx++) canvas.drawLine(gx * cell, gridTop, gx * cell, gridTop + cell * 9, paint);
        for (int gy = 0; gy <= 9; gy++) canvas.drawLine(0, gridTop + gy * cell, w, gridTop + gy * cell, paint);
        paint.setStyle(Paint.Style.FILL);

        // Шестерни
        for (String[] g : placed) {
            int x = Integer.parseInt(g[0]);
            int y = Integer.parseInt(g[1]);
            int r = Integer.parseInt(g[2]);
            drawGear(canvas, x * cell + cell / 2, gridTop + y * cell + cell / 2, r * cell, brass);
        }
        // Входная и выходная оси
        drawGear(canvas, 2 * cell + cell / 2, gridTop + 4 * cell + cell / 2, cell * 0.3f, copper);

        // Инвентарь шестерён
        float yInv = h * 0.82f;
        paint.setColor(textLight);
        paint.setTextSize(w * 0.038f);
        paint.setTextAlign(Paint.Align.LEFT);
        float xi = w * 0.05f;
        for (Map.Entry<Integer, Integer> e : remaining.entrySet()) {
            String label = "r=" + e.getKey() + " ×" + e.getValue();
            RectF r = new RectF(xi, yInv, xi + w * 0.22f, yInv + h * 0.07f);
            buttons.put("pick" + e.getKey(), r);
            paint.setColor(e.getKey() == selectedRadius ? brass : panel);
            canvas.drawRoundRect(r, 18, 18, paint);
            paint.setColor(textLight);
            canvas.drawText(label, xi + 16, yInv + h * 0.045f, paint);
            xi += w * 0.25f;
        }
        button(canvas, "check", "Проверить", w * 0.75f, h * 0.92f, w * 0.42f, h * 0.06f);
        button(canvas, "clear", "Сброс", w * 0.25f, h * 0.92f, w * 0.3f, h * 0.06f);
        button(canvas, "menu", "←", w * 0.93f, w * 0.06f, w * 0.1f, w * 0.06f);
    }

    private void drawGear(Canvas canvas, float cx, float cy, float radius, int color) {
        paint.setColor(color);
        canvas.drawCircle(cx, cy, radius, paint);
        // зубцы
        int teeth = Math.max(6, (int) (radius / 12));
        for (int i = 0; i < teeth; i++) {
            double ang = Math.PI * 2 * i / teeth;
            float tx = (float) (cx + Math.cos(ang) * radius * 1.12f);
            float ty = (float) (cy + Math.sin(ang) * radius * 1.12f);
            canvas.drawCircle(tx, ty, radius * 0.16f, paint);
        }
        paint.setColor(bg);
        canvas.drawCircle(cx, cy, radius * 0.3f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(radius * 0.08f);
        paint.setColor(color);
        canvas.drawCircle(cx, cy, radius * 0.62f, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawWin(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(Color.argb(200, 43, 33, 26));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(good);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.09f);
        canvas.drawText("МЕХАНИЗМ СОБРАН!", w / 2f, h * 0.4f, paint);
        paint.setColor(textLight);
        paint.setTextSize(w * 0.05f);
        canvas.drawText("ходов: " + moves + "  par: " + level.parMoves(), w / 2f, h * 0.47f, paint);
        button(canvas, "next", "Дальше", w / 2f, h * 0.58f, w * 0.5f, h * 0.07f);
        button(canvas, "menu", "К уровням", w / 2f, h * 0.68f, w * 0.5f, h * 0.07f);
    }

    private void drawMenu(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(brass);
        paint.setTextSize(w * 0.12f);
        canvas.drawText("МЕХАНОЗАВР", w / 2f, h * 0.3f, paint);
        paint.setColor(textLight);
        paint.setTextSize(w * 0.042f);
        canvas.drawText("стимпанк-пазл: собери зубчатую передачу", w / 2f, h * 0.36f, paint);
        canvas.drawText("с нужным передаточным числом", w / 2f, h * 0.41f, paint);
        button(canvas, "play", "Играть", w / 2f, h * 0.55f, w * 0.55f, h * 0.075f);
        paint.setTextSize(w * 0.038f);
        paint.setColor(copper);
        canvas.drawText("40 уровней · всего звёзд: " + progress.totalStars(), w / 2f, h * 0.66f, paint);
    }

    private void drawLevelSelect(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(brass);
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
            paint.setColor(unlocked ? brass : panel);
            canvas.drawRoundRect(r, 18, 18, paint);
            paint.setColor(textLight);
            paint.setTextSize(cw * 0.32f);
            canvas.drawText(String.valueOf(i), r.centerX(), r.centerY() + cw * 0.12f, paint);
        }
        button(canvas, "menu", "Назад", w / 2f, h * 0.95f, w * 0.45f, h * 0.05f);
    }

    private void button(Canvas canvas, String name, String label, float cx, float cy, float bw, float bh) {
        RectF r = new RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2);
        buttons.put(name, r);
        paint.setColor(copper);
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint);
        paint.setColor(textLight);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bh * 0.42f);
        canvas.drawText(label, cx, cy + bh * 0.15f, paint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) return true;
        float x = event.getX();
        float y = event.getY();

        for (Map.Entry<String, RectF> e : buttons.entrySet()) {
            if (!e.getValue().contains(x, y)) continue;
            String name = e.getKey();
            if (name.startsWith("pick")) {
                selectedRadius = Integer.parseInt(name.substring(4));
            } else if (name.startsWith("lvl")) {
                int lvl = Integer.parseInt(name.substring(3));
                if (progress.isUnlocked(lvl)) startLevel(lvl);
            } else if (name.equals("play")) {
                state = State.LevelSelect;
            } else if (name.equals("menu")) {
                state = (state == State.Win) ? State.LevelSelect : State.Menu;
            } else if (name.equals("next")) {
                if (currentLevel < 40) startLevel(currentLevel + 1);
                else state = State.LevelSelect;
            } else if (name.equals("check")) {
                if (system.size() >= 2 && system.connected(0, system.size() - 1)) {
                    double ratio = system.ratioFrom(0, system.size() - 1);
                    if (Math.abs(ratio - level.targetRatio) < 0.05) {
                        progress.complete(currentLevel, moves <= level.parMoves() ? 3 : moves <= level.parMoves() * 2 ? 2 : 1);
                        state = State.Win;
                    }
                }
            } else if (name.equals("clear")) {
                startLevel(currentLevel);
            }
            invalidate();
            return true;
        }

        if (state == State.Play) {
            float w = getWidth();
            float cell = w / 14f;
            float gridTop = getHeight() * 0.18f;
            int gx = (int) (x / cell);
            int gy = (int) ((y - gridTop) / cell);
            if (gx >= 0 && gx < 14 && gy >= 0 && gy < 9) {
                if (remaining.getOrDefault(selectedRadius, 0) > 0) {
                    Gear g = new Gear(gx, gy, selectedRadius);
                    if (system.place(g)) {
                        remaining.put(selectedRadius, remaining.get(selectedRadius) - 1);
                        placed.add(new String[]{String.valueOf(gx), String.valueOf(gy), String.valueOf(selectedRadius)});
                        moves++;
                    }
                }
                invalidate();
            }
        }
        return true;
    }

    /** Сохранение прогресса. */
    private static final class ProgressStore {
        private final android.content.SharedPreferences prefs;

        ProgressStore(Context context) {
            prefs = context.getSharedPreferences("gearworks", Context.MODE_PRIVATE);
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
