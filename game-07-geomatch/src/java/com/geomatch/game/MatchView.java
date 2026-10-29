package com.geomatch.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.List;

/**
 * «Гео-Матч» — match-3 с абстрактной геометрией: плитки-формы в стиле Баухаус.
 * Управление: тап по соседним плиткам — обмен.
 */
public final class MatchView extends View {

    private enum State { Menu, LevelSelect, Play, Win, Lose }

    private static final int[] TILE_COLORS = {
            Color.rgb(231, 76, 60),    // круг
            Color.rgb(46, 134, 193),   // треугольник
            Color.rgb(241, 196, 15),   // квадрат
            Color.rgb(39, 174, 96),    // пятиугольник
            Color.rgb(142, 68, 173),   // шестиугольник
            Color.rgb(230, 126, 34)    // ромб
    };

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private State state = State.Menu;
    private MatchBoard board;
    private GameSession session;
    private LevelCatalog.Params params;
    private int currentLevel = 1;
    private int cascadeLevel;
    private int selX = -1, selY = -1;
    private final java.util.Map<String, RectF> buttons = new java.util.LinkedHashMap<>();
    private final ProgressStore progress;

    private final int bg = Color.rgb(244, 241, 232);
    private final int panel = Color.rgb(44, 48, 60);
    private final int textLight = Color.rgb(240, 238, 230);

    public MatchView(Context context) {
        super(context);
        progress = new ProgressStore(context);
    }

    private void startLevel(int level) {
        currentLevel = level;
        params = LevelCatalog.paramsFor(level);
        board = new MatchBoard(8, 104729L * level + 7919L);
        session = new GameSession(params.moves, params.targetScore, params.targetColor);
        cascadeLevel = 0;
        selX = selY = -1;
        state = State.Play;
    }

    private void resolveMatches() {
        List<int[]> matches;
        while (!(matches = MatchLogic.findMatches(board.grid())).isEmpty()) {
            int gained = 0;
            for (int[] cell : matches) {
                int color = board.colorAt(cell[0], cell[1]);
                session.collectColor(color, 1);
                board.clearCell(cell[0], cell[1]);
                gained += 10;
            }
            session.addScore(MatchLogic.scoreFor(gained / 10, cascadeLevel));
            if (matches.size() >= 4) Sound.special();
            else Sound.match();
            cascadeLevel++;
            board.applyGravityAndRefill(System.nanoTime() + cascadeLevel);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawColor(bg);
        // сигнатура Баухаус: цветные угловые модули
        paint.setColor(TILE_COLORS[1]);
        canvas.drawRect(0, 0, getWidth() * 0.06f, getHeight() * 0.06f, paint);
        paint.setColor(TILE_COLORS[2]);
        canvas.drawCircle(getWidth() * 0.96f, getHeight() * 0.03f, getWidth() * 0.035f, paint);
        paint.setColor(TILE_COLORS[0]);
        canvas.drawRect(getWidth() * 0.93f, getHeight() * 0.94f, getWidth(), getHeight(), paint);
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
            case Lose:
                drawBoard(canvas);
                drawHud(canvas);
                if (state == State.Win) drawOverlay(canvas, "Уровень пройден!");
                if (state == State.Lose) drawOverlay(canvas, "Ходы закончились");
                break;
        }
    }

    private void drawBoard(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float boardSize = Math.min(w * 0.94f, h * 0.55f);
        float cell = boardSize / 8f;
        float left = (w - boardSize) / 2f;
        float top = h * 0.32f;

        paint.setColor(Color.rgb(226, 219, 202));
        canvas.drawRoundRect(new RectF(left - 8, top - 8, left + boardSize + 8, top + boardSize + 8), 24, 24, paint);

        for (int y = 0; y < 8; y++) {
            for (int x = 0; x < 8; x++) {
                float cx = left + (x + 0.5f) * cell;
                float cy = top + (y + 0.5f) * cell;
                int color = board.colorAt(x, y);
                boolean selected = (x == selX && y == selY);
                drawShape(canvas, cx, cy, cell * 0.42f, color, selected);
            }
        }
    }

    /** Рисует плитку-форму: цвет + геометрический символ. */
    private void drawShape(Canvas canvas, float cx, float cy, float r, int color, boolean selected) {
        paint.setColor(TILE_COLORS[Math.floorMod(color, TILE_COLORS.length)]);
        Path path = new Path();
        int sides = 3 + Math.floorMod(color, 4); // 3..6 сторон
        for (int i = 0; i < sides; i++) {
            double angle = Math.PI * 2 * i / sides - Math.PI / 2 + (selected ? 0.2 : 0);
            float px = (float) (cx + Math.cos(angle) * r);
            float py = (float) (cy + Math.sin(angle) * r);
            if (i == 0) path.moveTo(px, py);
            else path.lineTo(px, py);
        }
        path.close();
        canvas.drawPath(path, paint);
        if (selected) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(8);
            paint.setColor(Color.WHITE);
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void drawHud(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(panel);
        canvas.drawRoundRect(new RectF(0, 0, w, h * 0.28f), 0, 0, paint);
        paint.setColor(textLight);
        paint.setTextSize(w * 0.05f);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Уровень " + currentLevel, w * 0.04f, w * 0.09f, paint);
        paint.setTextSize(w * 0.038f);
        canvas.drawText("Счёт: " + session.score() + " / " + params.targetScore, w * 0.04f, w * 0.145f, paint);
        canvas.drawText("Цель: " + params.targetCount + " фишек цвета " + (params.targetColor + 1)
                + " (собрано " + session.collected() + ")", w * 0.04f, w * 0.19f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setTextSize(w * 0.05f);
        canvas.drawText("Ходы: " + session.movesLeft(), w * 0.96f, w * 0.09f, paint);
    }

    private void drawOverlay(Canvas canvas, String title) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(Color.argb(170, 30, 30, 40));
        canvas.drawRect(0, 0, w, h, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.08f);
        canvas.drawText(title, w / 2, h * 0.42f, paint);
        if (state == State.Win) {
            paint.setTextSize(w * 0.05f);
            canvas.drawText("★ " + GameSession.starsFor(session.score(), params.targetScore), w / 2, h * 0.48f, paint);
            button(canvas, "next", "Дальше", w / 2, h * 0.58f, w * 0.5f, h * 0.07f);
        } else {
            button(canvas, "retry", "Ещё раз", w / 2, h * 0.58f, w * 0.5f, h * 0.07f);
        }
        button(canvas, "menu", "В меню", w / 2, h * 0.68f, w * 0.5f, h * 0.07f);
    }

    private void drawMenu(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(panel);
        paint.setTextSize(w * 0.11f);
        canvas.drawText("ГЕО-МАТЧ", w / 2, h * 0.3f, paint);
        paint.setTextSize(w * 0.042f);
        paint.setColor(Color.rgb(100, 100, 110));
        canvas.drawText("match-3 из абстрактных форм", w / 2, h * 0.36f, paint);
        button(canvas, "play", "Играть", w / 2, h * 0.52f, w * 0.55f, h * 0.075f);
        paint.setTextSize(w * 0.038f);
        paint.setColor(Color.rgb(120, 120, 130));
        canvas.drawText("всего звёзд: " + progress.totalStars() + " / " + LevelCatalog.LEVEL_COUNT * 3, w / 2, h * 0.63f, paint);
    }

    private void drawLevelSelect(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(panel);
        paint.setTextSize(w * 0.08f);
        canvas.drawText("Уровни", w / 2, h * 0.08f, paint);
        for (int i = 1; i <= LevelCatalog.LEVEL_COUNT; i++) {
            int col = (i - 1) % 5;
            int row = (i - 1) / 5;
            float cw = w * 0.17f;
            float x = w * 0.05f + col * w * 0.19f;
            float y = h * 0.12f + row * h * 0.088f;
            RectF r = new RectF(x, y, x + cw, y + h * 0.07f);
            buttons.put("lvl" + i, r);
            boolean unlocked = progress.isUnlocked(i);
            paint.setColor(unlocked ? Color.rgb(46, 134, 193) : Color.rgb(190, 186, 175));
            canvas.drawRoundRect(r, 20, 20, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(cw * 0.3f);
            canvas.drawText(String.valueOf(i), r.centerX(), r.centerY() + cw * 0.11f, paint);
            int stars = progress.starsFor(i);
            for (int s = 0; s < stars; s++) {
                paint.setColor(Color.rgb(241, 196, 15));
                canvas.drawCircle(r.centerX() + (s - 1) * cw * 0.16f, r.bottom - 8, cw * 0.06f, paint);
            }
        }
        button(canvas, "menu", "Назад", w / 2, h * 0.95f, w * 0.45f, h * 0.05f);
    }

    private void button(Canvas canvas, String name, String label, float cx, float cy, float bw, float bh) {
        RectF r = new RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2);
        buttons.put(name, r);
        paint.setColor(Color.rgb(231, 76, 60));
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint);
        paint.setColor(Color.WHITE);
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
                int level = Integer.parseInt(name.substring(3));
                if (progress.isUnlocked(level)) startLevel(level);
            } else if (name.equals("play")) {
                state = State.LevelSelect;
            } else if (name.equals("menu")) {
                state = State.Menu;
            } else if (name.equals("retry")) {
                startLevel(currentLevel);
            } else if (name.equals("next")) {
                if (currentLevel < LevelCatalog.LEVEL_COUNT) startLevel(currentLevel + 1);
                else state = State.LevelSelect;
            }
            invalidate();
            return true;
        }

        if (state == State.Play) {
            float w = getWidth();
            float h = getHeight();
            float boardSize = Math.min(w * 0.94f, h * 0.55f);
            float cell = boardSize / 8f;
            float left = (w - boardSize) / 2f;
            float top = h * 0.32f;
            int bx = (int) ((x - left) / cell);
            int by = (int) ((y - top) / cell);
            if (bx >= 0 && bx < 8 && by >= 0 && by < 8) {
                if (selX < 0) {
                    selX = bx;
                    selY = by;
                } else if (Math.abs(selX - bx) + Math.abs(selY - by) == 1) {
                    if (board.swap(selX, selY, bx, by)) {
                        Sound.swap();
                        cascadeLevel = 0;
                        session.useMove();
                        resolveMatches();
                        if (session.isObjectiveMet(params.targetCount)) {
                            progress.complete(currentLevel, GameSession.starsFor(session.score(), params.targetScore));
                            Sound.win();
                            state = State.Win;
                        } else if (!session.hasMovesLeft()) {
                            state = State.Lose;
                        }
                    }
                    selX = selY = -1;
                } else {
                    selX = bx;
                    selY = by;
                }
                invalidate();
            }
        }
        return true;
    }

    /** Сохранение прогресса (уровни и звёзды) в SharedPreferences. */
    private static final class ProgressStore {
        private final android.content.SharedPreferences prefs;

        ProgressStore(Context context) {
            prefs = context.getSharedPreferences("geomatch", Context.MODE_PRIVATE);
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
            for (int i = 1; i <= LevelCatalog.LEVEL_COUNT; i++) total += starsFor(i);
            return total;
        }
    }
}
