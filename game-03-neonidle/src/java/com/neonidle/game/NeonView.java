package com.neonidle.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Экран «Неоновое ядро»: тёмный фон, светящийся реактор, панели покупок.
 * Вся графика — Canvas-примитивы с «свечением» (полупрозрачные круги).
 */
public final class NeonView extends View {

    private final Game game = new Game();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final List<float[]> floats = new ArrayList<>(); // всплывающие числа
    private long lastTick = SystemClock.elapsedRealtime();
    private int tab = 0; // 0 генераторы, 1 апгрейды, 2 статистика
    private final List<RectF> rowRects = new ArrayList<>();
    private final List<Integer> rowIds = new ArrayList<>();
    private RectF coreRect = new RectF();
    private RectF tabRect0 = new RectF();
    private RectF tabRect1 = new RectF();
    private RectF tabRect2 = new RectF();
    private RectF prestigeRect = new RectF();

    private final int bg = Color.rgb(8, 10, 12);
    private final int neon = Color.rgb(0, 255, 200);
    private final int neon2 = Color.rgb(255, 170, 40);
    private final int dim = Color.rgb(70, 76, 110);
    private final int textLight = Color.rgb(220, 226, 255);

    public NeonView(Context context) {
        super(context);
        load();
    }

    private void load() {
        String s = getContext().getSharedPreferences("neonidle", Context.MODE_PRIVATE)
                .getString("save", "");
        Game loaded = Game.load(s);
        double offline = 0;
        if (!s.isEmpty()) {
            long savedAt = getContext().getSharedPreferences("neonidle", Context.MODE_PRIVATE)
                    .getLong("savedAt", 0);
            long now = System.currentTimeMillis();
            if (savedAt > 0 && now > savedAt) {
                offline = loaded.offlineGain((now - savedAt) / 1000.0);
            }
        }
        copyFrom(loaded);
        if (offline > 0) {
            game.addEnergy(offline);
        }
    }

    private void copyFrom(Game src) {
        // Загрузка через сериализацию состояния
        String save = src.save();
        Game restored = Game.load(save);
        game.restoreFrom(restored);
    }

    public void save() {
        game.checkAchievements();
        getContext().getSharedPreferences("neonidle", Context.MODE_PRIVATE).edit()
                .putString("save", game.save())
                .putLong("savedAt", System.currentTimeMillis())
                .apply();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.elapsedRealtime();
        double dt = (now - lastTick) / 1000.0;
        lastTick = now;
        game.tick(dt);

        canvas.drawColor(bg);
        drawGrid(canvas);
        drawCore(canvas, now);
        drawStats(canvas);
        drawTabs(canvas);
        if (tab == 0) {
            drawGenerators(canvas);
        } else if (tab == 1) {
            drawUpgrades(canvas);
        } else {
            drawStatsPanel(canvas);
        }
        drawFloats(canvas);

        // автообновление ~10 FPS
        postInvalidateDelayed(90);
    }

    private void drawGrid(Canvas canvas) {
        paint.setColor(Color.rgb(22, 24, 48));
        paint.setStrokeWidth(1);
        int step = getWidth() / 12;
        for (int x = 0; x < getWidth(); x += step) {
            canvas.drawLine(x, 0, x, getHeight(), paint);
        }
        for (int y = 0; y < getHeight(); y += step) {
            canvas.drawLine(0, y, getWidth(), y, paint);
        }
    }

    private void drawCore(Canvas canvas, long now) {
        float w = getWidth();
        float cx = w / 2f;
        float cy = w * 0.52f;
        float r = w * 0.19f;
        float pulse = 1f + 0.05f * (float) Math.sin(now / 350.0);

        // свечение
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new RadialGradient(cx, cy, r * 2.2f * pulse,
                new int[]{Color.argb(120, 0, 255, 200), Color.argb(0, 0, 255, 200)},
                new float[]{0f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawCircle(cx, cy, r * 2.2f * pulse, paint);
        paint.setShader(null);

        paint.setColor(Color.rgb(14, 18, 40));
        canvas.drawCircle(cx, cy, r * pulse, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(w * 0.012f);
        paint.setColor(neon);
        canvas.drawCircle(cx, cy, r * pulse, paint);
        paint.setColor(neon2);
        canvas.drawCircle(cx, cy, r * 0.62f * pulse, paint);
        paint.setStyle(Paint.Style.FILL);

        paint.setColor(textLight);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.055f);
        canvas.drawText("+" + Game.formatNumber(game.clickValue()), cx, cy + w * 0.018f, paint);
        paint.setTextSize(w * 0.032f);
        paint.setColor(dim);
        canvas.drawText("нажми ядро", cx, cy + w * 0.075f, paint);

        coreRect = new RectF(cx - r * 2.2f, cy - r * 2.2f, cx + r * 2.2f, cy + r * 2.2f);
    }

    private void drawStats(Canvas canvas) {
        float w = getWidth();
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(neon);
        paint.setTextSize(w * 0.052f);
        canvas.drawText("⚡ " + Game.formatNumber(game.energy()), w * 0.05f, w * 0.115f, paint);
        paint.setColor(textLight);
        paint.setTextSize(w * 0.034f);
        canvas.drawText("в секунду: " + Game.formatNumber(game.energyPerSecond()), w * 0.05f, w * 0.16f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(neon2);
        canvas.drawText("◆ " + game.shards() + " осколков", w * 0.95f, w * 0.115f, paint);
        paint.setColor(dim);
        paint.setTextSize(w * 0.03f);
        canvas.drawText("престиж ×" + String.format(java.util.Locale.US, "%.2f", game.shardMultiplier()),
                w * 0.95f, w * 0.16f, paint);
    }

    private void drawTabs(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float top = h * 0.735f;
        float tw = w * 0.3f;
        tabRect0 = new RectF(w * 0.025f, top, w * 0.025f + tw, top + h * 0.052f);
        tabRect1 = new RectF(w * 0.35f, top, w * 0.35f + tw, top + h * 0.052f);
        tabRect2 = new RectF(w * 0.675f, top, w * 0.675f + tw, top + h * 0.052f);
        drawButton(canvas, tabRect0, "Генераторы", tab == 0);
        drawButton(canvas, tabRect1, "Апгрейды", tab == 1);
        drawButton(canvas, tabRect2, "Престиж", tab == 2);
    }

    private void drawButton(Canvas canvas, RectF r, String label, boolean active) {
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? Color.rgb(0, 90, 80) : Color.rgb(24, 28, 56));
        canvas.drawRoundRect(r, 20, 20, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(active ? neon : dim);
        canvas.drawRoundRect(r, 20, 20, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(active ? neon : textLight);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(r.height() * 0.42f);
        canvas.drawText(label, r.centerX(), r.centerY() + r.height() * 0.15f, paint);
    }

    private void drawGenerators(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        rowRects.clear();
        rowIds.clear();
        float rowH = h * 0.045f;
        float top = h * 0.80f;
        for (int i = 0; i < Game.generators().length; i++) {
            GeneratorDef d = Game.generators()[i];
            RectF r = new RectF(w * 0.03f, top, w * 0.97f, top + rowH * 0.92f);
            rowRects.add(r);
            rowIds.add(i);
            double cost = d.costFor(game.generatorCount(i));
            boolean affordable = game.energy() >= cost;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(affordable ? Color.rgb(16, 40, 48) : Color.rgb(18, 20, 42));
            canvas.drawRoundRect(r, 16, 16, paint);
            paint.setColor(affordable ? neon : dim);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setTextSize(rowH * 0.34f);
            canvas.drawText(d.name + "  ×" + game.generatorCount(i), r.left + rowH * 0.3f, r.centerY() + rowH * 0.12f, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(affordable ? textLight : dim);
            canvas.drawText(Game.formatNumber(cost) + " ⚡", r.right - rowH * 0.3f, r.centerY() + rowH * 0.12f, paint);
            top += rowH;
        }
    }

    private void drawUpgrades(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        rowRects.clear();
        rowIds.clear();
        float rowH = h * 0.038f;
        float top = h * 0.80f;
        for (int i = 0; i < Game.upgradeCount(); i++) {
            RectF r = new RectF(w * 0.03f, top, w * 0.97f, top + rowH * 0.9f);
            rowRects.add(r);
            rowIds.add(100 + i);
            boolean owned = game.hasUpgrade(i);
            boolean affordable = game.energy() >= Game.upgradeCost(i);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(owned ? Color.rgb(20, 60, 50) : affordable ? Color.rgb(40, 24, 56) : Color.rgb(18, 20, 42));
            canvas.drawRoundRect(r, 14, 14, paint);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(owned ? neon : textLight);
            paint.setTextSize(rowH * 0.36f);
            canvas.drawText(Game.upgradeName(i), r.left + rowH * 0.3f, r.centerY() + rowH * 0.13f, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setColor(owned ? dim : affordable ? neon2 : dim);
            canvas.drawText(owned ? "куплено" : Game.formatNumber(Game.upgradeCost(i)) + " ⚡",
                    r.right - rowH * 0.3f, r.centerY() + rowH * 0.13f, paint);
            top += rowH;
        }
    }

    private void drawStatsPanel(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        float top = h * 0.80f;
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setColor(textLight);
        paint.setTextSize(w * 0.036f);
        canvas.drawText("Всего заработано: " + Game.formatNumber(game.totalEarned()), w * 0.05f, top, paint);
        canvas.drawText("Престижей: " + game.prestigeCount() + "   Осколков: " + game.shards(), w * 0.05f, top + w * 0.05f, paint);
        Set<String> ach = game.achievements();
        canvas.drawText("Достижения (" + ach.size() + "/10):", w * 0.05f, top + w * 0.10f, paint);
        paint.setColor(ach.isEmpty() ? dim : neon);
        canvas.drawText(ach.isEmpty() ? "пока пусто — покупай генераторы!" : ach.toString(),
                w * 0.05f, top + w * 0.145f, paint);

        prestigeRect = new RectF(w * 0.15f, top + w * 0.22f, w * 0.85f, top + w * 0.32f);
        boolean can = game.canPrestige();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(can ? Color.rgb(90, 20, 60) : Color.rgb(28, 28, 52));
        canvas.drawRoundRect(prestigeRect, 24, 24, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);
        paint.setColor(can ? neon2 : dim);
        canvas.drawRoundRect(prestigeRect, 24, 24, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(can ? neon2 : dim);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(prestigeRect.height() * 0.3f);
        String label = can
                ? "ПРЕСТИЖ: +" + Game.shardsFor(game.totalEarned()) + " осколков"
                : "престиж откроется на 1.0B энергии";
        canvas.drawText(label, prestigeRect.centerX(), prestigeRect.centerY() + prestigeRect.height() * 0.12f, paint);
    }

    private void drawFloats(Canvas canvas) {
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = floats.size() - 1; i >= 0; i--) {
            float[] f = floats.get(i);
            f[1] -= getHeight() * 0.004f;
            f[2] -= 0.02f;
            if (f[2] <= 0) {
                floats.remove(i);
                continue;
            }
            paint.setColor(Color.argb((int) (255 * f[2]), 0, 255, 200));
            paint.setTextSize(getWidth() * 0.042f);
            canvas.drawText(Game.formatNumber(f[3]), f[0], f[1], paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) {
            return true;
        }
        float x = event.getX();
        float y = event.getY();
        if (coreRect.contains(x, y)) {
            game.click();
            floats.add(new float[]{x, y, 1f, (float) game.clickValue()});
            Synth.click();
            return true;
        }
        if (tabRect0.contains(x, y)) {
            tab = 0;
            invalidate();
            return true;
        }
        if (tabRect1.contains(x, y)) {
            tab = 1;
            invalidate();
            return true;
        }
        if (tabRect2.contains(x, y)) {
            tab = 2;
            invalidate();
            return true;
        }
        if (tab == 2 && prestigeRect.contains(x, y)) {
            if (game.canPrestige()) {
                game.prestige();
                game.checkAchievements();
                Synth.prestige();
                save();
            }
            invalidate();
            return true;
        }
        for (int i = 0; i < rowRects.size(); i++) {
            if (rowRects.get(i).contains(x, y)) {
                int id = rowIds.get(i);
                boolean bought;
                if (id >= 100) {
                    bought = game.buyUpgrade(id - 100);
                } else {
                    bought = game.buyGenerator(id);
                }
                if (bought) {
                    Synth.buy();
                    game.checkAchievements();
                }
                invalidate();
                return true;
            }
        }
        return true;
    }
}
