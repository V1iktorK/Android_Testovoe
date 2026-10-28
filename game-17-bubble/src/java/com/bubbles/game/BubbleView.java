package com.bubbles.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * «Пузырьки глубин» — bubble shooter в подводном мире: стреляй пузырями, собирай цепочки.
 * Стиль: глубокие синие тона, кораллы, световые столбы с поверхности.
 */
public final class BubbleView extends View {

    private enum State { Menu, LevelSelect, Play, Win, Lose }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private State state = State.Menu;
    private BubbleBoard board;
    private int currentLevel = 1;
    private int score = 0;
    private int shotsLeft;
    private int nextColor;
    private int chain = 1;
    private final java.util.Map<String, RectF> buttons = new java.util.LinkedHashMap<>();
    private final ProgressStore progress;

    private static final int[] BUBBLE_COLORS = {
            Color.rgb(255, 111, 97), Color.rgb(255, 183, 77), Color.rgb(129, 222, 164),
            Color.rgb(94, 168, 255), Color.rgb(186, 122, 255)
    };

    private final int deepBlue = Color.rgb(12, 40, 74);
    private final int midBlue = Color.rgb(22, 74, 120);
    private final int textLight = Color.rgb(222, 240, 255);
    private final int coral = Color.rgb(255, 140, 120);

    public BubbleView(Context context) {
        super(context);
        progress = new ProgressStore(context);
    }

    private void startLevel(int levelNumber) {
        currentLevel = levelNumber;
        board = BubbleBoard.generate(20261028L + levelNumber * 53, 3 + levelNumber / 3);
        shotsLeft = 12 + levelNumber / 2;
        nextColor = (int) (Math.random() * BubbleBoard.COLORS);
        score = 0;
        chain = 1;
        state = State.Play;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(deepBlue);
        drawBackground(canvas);
        buttons.clear();
        switch (state) {
            case Menu -> drawMenu(canvas);
            case LevelSelect -> drawLevelSelect(canvas);
            case Play, Win, Lose -> {
                drawBoard(canvas);
                drawHud(canvas);
                if (state == State.Win) drawOverlay(canvas, "Уровень пройден!");
                if (state == State.Lose) drawOverlay(canvas, "Ходы закончились");
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        // световые столбы
        paint.setColor(Color.argb(26, 120, 200, 255));
        canvas.drawOval(new RectF(w * 0.1f, -h * 0.2f, w * 0.5f, h * 0.7f), paint);
        canvas.drawOval(new RectF(w * 0.55f, -h * 0.1f, w * 0.95f, h * 0.6f), paint);
        // кораллы
        paint.setColor(coral);
        canvas.drawCircle(w * 0.12f, h * 0.97f, w * 0.08f, paint);
        canvas.drawCircle(w * 0.2f, h * 0.98f, w * 0.06f, paint);
        paint.setColor(Color.rgb(129, 222, 164));
        canvas.drawCircle(w * 0.86f, h * 0.98f, w * 0.09f, paint);
    }

    private void drawBoard(Canvas canvas) {
        float w = getWidth();
        float cell = w / BubbleBoard.WIDTH;
        float top = h(0.16f);
        int[][] grid = board.grid();
        for (int y = 0; y < board.rows(); y++) {
            for (int x = 0; x < BubbleBoard.WIDTH; x++) {
                if (grid[y][x] < 0) continue;
                float cx = (x + 0.5f) * cell + (y % 2 == 0 ? 0 : cell * 0.5f);
                float cy = top + y * cell * 0.92f;
                drawBubble(canvas, cx, cy, cell * 0.44f, BUBBLE_COLORS[grid[y][x]]);
            }
        }
        // пушка и следующий пузырёк
        float launcherY = h(0.88f);
        drawBubble(canvas, w / 2f, launcherY, cell * 0.5f, BUBBLE_COLORS[nextColor]);
        paint.setColor(Color.rgb(40, 90, 140));
        canvas.drawCircle(w / 2f, launcherY + cell * 0.55f, cell * 0.32f, paint);
    }

    private void drawBubble(Canvas canvas, float cx, float cy, float r, int color) {
        paint.setColor(color);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawCircle(cx - r * 0.3f, cy - r * 0.35f, r * 0.32f, paint);
    }

    private void drawHud(Canvas canvas) {
        float w = getWidth();
        paint.setColor(textLight);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(w * 0.05f);
        canvas.drawText("Уровень " + currentLevel, w * 0.04f, w * 0.09f, paint);
        paint.setTextSize(w * 0.038f);
        canvas.drawText("очки: " + score + "   ходов: " + shotsLeft, w * 0.04f, w * 0.14f, paint);
    }

    private void drawOverlay(Canvas canvas, String title) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(Color.argb(180, 8, 24, 44));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(textLight);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.08f);
        canvas.drawText(title, w / 2f, h * 0.42f, paint);
        if (state == State.Win) {
            button(canvas, "next", "Дальше", w / 2f, h * 0.55f, w * 0.5f, h * 0.07f);
        } else {
            button(canvas, "retry", "Ещё раз", w / 2f, h * 0.55f, w * 0.5f, h * 0.07f);
        }
        button(canvas, "menu", "В меню", w / 2f, h * 0.65f, w * 0.5f, h * 0.07f);
    }

    private void drawMenu(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(textLight);
        paint.setTextSize(w * 0.1f);
        canvas.drawText("ПУЗЫРЬКИ ГЛУБИН", w / 2f, h * 0.3f, paint);
        paint.setTextSize(w * 0.042f);
        canvas.drawText("подводный bubble shooter: собери цепочки", w / 2f, h * 0.36f, paint);
        button(canvas, "play", "Играть", w / 2f, h * 0.52f, w * 0.55f, h * 0.075f);
        paint.setTextSize(w * 0.038f);
        paint.setColor(Color.rgb(140, 190, 230));
        canvas.drawText("30 уровней · всего звёзд: " + progress.totalStars(), w / 2f, h * 0.63f, paint);
    }

    private void drawLevelSelect(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(textLight);
        paint.setTextSize(w * 0.08f);
        canvas.drawText("Уровни", w / 2f, h * 0.08f, paint);
        for (int i = 1; i <= 30; i++) {
            int col = (i - 1) % 5;
            int row = (i - 1) / 5;
            float cw = w * 0.17f;
            float x = w * 0.05f + col * w * 0.19f;
            float y = h * 0.12f + row * h * 0.1f;
            RectF r = new RectF(x, y, x + cw, y + h * 0.08f);
            buttons.put("lvl" + i, r);
            boolean unlocked = progress.isUnlocked(i);
            paint.setColor(unlocked ? Color.rgb(94, 168, 255) : Color.rgb(30, 60, 100));
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
        paint.setColor(Color.rgb(94, 168, 255));
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint);
        paint.setColor(Color.WHITE);
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
                state = State.LevelSelect;
            } else if (name.equals("next")) {
                if (currentLevel < 30) startLevel(currentLevel + 1);
                else state = State.LevelSelect;
            } else if (name.equals("retry")) {
                startLevel(currentLevel);
            }
            invalidate();
            return true;
        }

        if (state == State.Play && y > h(0.16f)) {
            float w = getWidth();
            float cell = w / BubbleBoard.WIDTH;
            int col = (int) (x / cell);
            if (col >= 0 && col < BubbleBoard.WIDTH) {
                board.shoot(col, nextColor);
                // ищем поставленный пузырёк и очищаем цепочку
                int placedRow = -1;
                for (int r = 0; r < board.rows(); r++) {
                    if (board.grid()[r][col] == nextColor) {
                        placedRow = r;
                    }
                }
                if (placedRow >= 0) {
                    int cleared = board.resolveAt(col, placedRow);
                    if (cleared > 0) {
                        score += BubbleLogic.scoreFor(cleared, chain);
                        chain = Math.min(9, chain + 1);
                    } else {
                        chain = 1;
                    }
                }
                nextColor = (int) (Math.random() * BubbleBoard.COLORS);
                shotsLeft--;
                if (board.isCleared()) {
                    progress.complete(currentLevel, shotsLeft > 6 ? 3 : shotsLeft > 2 ? 2 : 1);
                    state = State.Win;
                } else if (shotsLeft <= 0) {
                    state = State.Lose;
                }
                invalidate();
            }
        }
        return true;
    }

    private float h(float frac) {
        return getHeight() * frac;
    }

    /** Сохранение прогресса. */
    private static final class ProgressStore {
        private final android.content.SharedPreferences prefs;

        ProgressStore(Context context) {
            prefs = context.getSharedPreferences("bubbles", Context.MODE_PRIVATE);
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
            for (int i = 1; i <= 30; i++) total += starsFor(i);
            return total;
        }
    }
}
