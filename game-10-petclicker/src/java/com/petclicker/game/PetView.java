package com.petclicker.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * «Пушистый кликер» — тапы по милому зверьку, покупка питомцев, ивенты.
 * Милый стиль: пастельные тона, процедурные зверята из примитивов Canvas.
 */
public final class PetView extends View {

    private enum State { Menu, Play, Shop }

    private final PetGame game = new PetGame();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private State state = State.Play;
    private long lastTick = SystemClock.elapsedRealtime();
    private double eventTimer = 6.0;
    private EventSpawner.Type activeEvent;
    private double eventFlash;
    private final List<float[]> floats = new ArrayList<>();
    private final List<String> shopRows = new ArrayList<>();
    private final List<RectF> buttons = new ArrayList<>();
    private final List<Integer> buttonIds = new ArrayList<>();
    private RectF petRect = new RectF();
    private PetSound sound = new PetSound();

    // Пастельная палитра
    private final int bg = Color.rgb(255, 243, 224);
    private final int card = Color.rgb(255, 255, 255);
    private final int textBrown = Color.rgb(94, 66, 52);
    private final int accent = Color.rgb(255, 159, 125);
    private final int mint = Color.rgb(164, 221, 192);

    public PetView(Context context) {
        super(context);
        load();
    }

    private void load() {
        String save = getContext().getSharedPreferences("petclicker", Context.MODE_PRIVATE).getString("save", "");
        PetGame loaded = PetGame.load(save);
        game.restoreFrom(loaded);
    }

    private void save() {
        getContext().getSharedPreferences("petclicker", Context.MODE_PRIVATE)
                .edit().putString("save", game.save()).apply();
    }

    /** Публичное сохранение для жизненного цикла Activity. */
    public void saveNow() {
        save();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.elapsedRealtime();
        double dt = Math.min((now - lastTick) / 1000.0, 0.1);
        lastTick = now;
        game.tick(dt);
        eventTimer -= dt;
        if (eventTimer <= 0) {
            activeEvent = EventSpawner.Type.values()[(int) (Math.random() * 4)];
            eventFlash = 1.0;
            eventTimer = 12 + Math.random() * 12;
        }
        if (eventFlash > 0) eventFlash -= dt * 0.6;

        canvas.drawColor(bg);
        buttons.clear();
        buttonIds.clear();

        drawHeader(canvas);
        drawPet(canvas, now);
        drawFloats(canvas);
        if (state == State.Shop) drawShop(canvas);
        else drawActions(canvas);
        if (eventFlash > 0) drawEventBanner(canvas);

        postInvalidateDelayed(80);
    }

    private void drawHeader(Canvas canvas) {
        float w = getWidth();
        paint.setColor(textBrown);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(w * 0.06f);
        canvas.drawText("🪙 " + format(game.coins()), w * 0.05f, w * 0.1f, paint);
        paint.setTextSize(w * 0.038f);
        canvas.drawText("в секунду: " + format(game.coinsPerSecond()), w * 0.05f, w * 0.155f, paint);
        paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(accent);
        canvas.drawText("комбо ×" + game.combo(), w * 0.95f, w * 0.1f, paint);
        paint.setColor(mint);
        canvas.drawText("уровень " + game.level(), w * 0.95f, w * 0.155f, paint);
    }

    private void drawPet(Canvas canvas, long now) {
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h * 0.45f;
        float r = w * 0.22f;
        float bounce = (float) (Math.sin(now / 350.0) * r * 0.04);
        petRect = new RectF(cx - r, cy - r + bounce, cx + r, cy + r + bounce);

        // тень
        paint.setColor(Color.argb(30, 94, 66, 52));
        canvas.drawOval(new RectF(cx - r * 0.8f, cy + r * 0.9f, cx + r * 0.8f, cy + r * 1.15f), paint);

        // тело
        paint.setColor(Color.rgb(255, 214, 170));
        canvas.drawOval(new RectF(cx - r * 0.85f, cy - r * 0.25f + bounce, cx + r * 0.85f, cy + r + bounce), paint);
        // ушки
        paint.setColor(Color.rgb(255, 196, 150));
        Path earL = new Path();
        earL.moveTo(cx - r * 0.75f, cy - r * 0.35f + bounce);
        earL.lineTo(cx - r * 0.55f, cy - r * 1.15f + bounce);
        earL.lineTo(cx - r * 0.25f, cy - r * 0.55f + bounce);
        earL.close();
        canvas.drawPath(earL, paint);
        Path earR = new Path();
        earR.moveTo(cx + r * 0.75f, cy - r * 0.35f + bounce);
        earR.lineTo(cx + r * 0.55f, cy - r * 1.15f + bounce);
        earR.lineTo(cx + r * 0.25f, cy - r * 0.55f + bounce);
        earR.close();
        canvas.drawPath(earR, paint);
        // голова
        paint.setColor(Color.rgb(255, 222, 183));
        canvas.drawCircle(cx, cy - r * 0.25f + bounce, r * 0.72f, paint);
        // глаза
        paint.setColor(textBrown);
        canvas.drawCircle(cx - r * 0.28f, cy - r * 0.32f + bounce, r * 0.09f, paint);
        canvas.drawCircle(cx + r * 0.28f, cy - r * 0.32f + bounce, r * 0.09f, paint);
        // нос и улыбка
        paint.setColor(Color.rgb(235, 130, 120));
        canvas.drawCircle(cx, cy - r * 0.12f + bounce, r * 0.08f, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(r * 0.05f);
        canvas.drawArc(new RectF(cx - r * 0.22f, cy - r * 0.18f + bounce, cx + r * 0.22f, cy + r * 0.12f + bounce),
                20, 140, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    private void drawFloats(Canvas canvas) {
        paint.setTextAlign(Paint.Align.CENTER);
        for (int i = floats.size() - 1; i >= 0; i--) {
            float[] f = floats.get(i);
            f[1] -= getHeight() * 0.005f;
            f[2] -= 0.02f;
            if (f[2] <= 0) {
                floats.remove(i);
                continue;
            }
            paint.setColor(Color.argb((int) (255 * f[2]), 235, 130, 120));
            paint.setTextSize(getWidth() * 0.05f);
            canvas.drawText("+" + format(f[3]), f[0], f[1], paint);
        }
    }

    private void drawActions(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        button(canvas, 0, "Магазин питомцев", w / 2, h * 0.68f, w * 0.62f, h * 0.065f);
        button(canvas, 1, "Сохранить", w / 2, h * 0.77f, w * 0.62f, h * 0.065f);
        paint.setColor(textBrown);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.036f);
        canvas.drawText("тап по зверьку — монеты · комбо растёт от быстрых тапов", w / 2, h * 0.86f, paint);
    }

    private void drawShop(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(card);
        canvas.drawRoundRect(new RectF(w * 0.05f, h * 0.22f, w * 0.95f, h * 0.82f), 30, 30, paint);
        paint.setColor(textBrown);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.055f);
        canvas.drawText("Магазин питомцев", w / 2, h * 0.27f, paint);
        for (int i = 0; i < PetGame.PET_COUNT; i++) {
            float y = h * 0.32f + i * h * 0.058f;
            boolean unlocked = PetGame.isPetUnlocked(i, game.level());
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(unlocked ? textBrown : Color.rgb(180, 170, 160));
            paint.setTextSize(w * 0.036f);
            canvas.drawText(petName(i) + "  ×" + game.petCount(i), w * 0.1f, y, paint);
            paint.setTextAlign(Paint.Align.RIGHT);
            if (unlocked) {
                paint.setColor(Color.rgb(120, 190, 150));
                canvas.drawText(format(game.petCost(i)) + " 🪙", w * 0.88f, y, paint);
                RectF r = new RectF(w * 0.1f, y - h * 0.035f, w * 0.88f, y + h * 0.012f);
                buttons.add(r);
                buttonIds.add(100 + i);
            } else {
                paint.setColor(Color.rgb(180, 170, 160));
                canvas.drawText("откроется на ур. " + (1 + i * 2), w * 0.88f, y, paint);
            }
        }
        button(canvas, 2, "Назад", w / 2, h * 0.87f, w * 0.5f, h * 0.055f);
    }

    private void drawEventBanner(Canvas canvas) {
        float w = getWidth();
        paint.setColor(Color.argb((int) (200 * eventFlash), 255, 214, 120));
        canvas.drawRoundRect(new RectF(w * 0.1f, w * 0.2f, w * 0.9f, w * 0.32f), 24, 24, paint);
        paint.setColor(textBrown);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.05f);
        canvas.drawText("Событие! " + eventName(activeEvent), w / 2, w * 0.33f, paint);
        paint.setTextSize(w * 0.036f);
        canvas.drawText("тап — забрать награду", w / 2, w * 0.41f, paint);
    }

    private void button(Canvas canvas, int id, String label, float cx, float cy, float bw, float bh) {
        RectF r = new RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2);
        buttons.add(r);
        buttonIds.add(id);
        paint.setColor(accent);
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

        if (eventFlash > 0 && eventFlash > 0.2) {
            game.triggerEvent(activeEvent);
            sound.event();
            eventFlash = 0;
            save();
            return true;
        }

        if (petRect.contains(x, y)) {
            game.tap();
            floats.add(new float[]{x, y, 1f, (float) game.tapValue(Math.max(0, game.combo() - 1))});
            sound.tap();
            return true;
        }

        for (int i = 0; i < buttons.size(); i++) {
            if (!buttons.get(i).contains(x, y)) continue;
            int id = buttonIds.get(i);
            if (id == 0) state = State.Shop;
            else if (id == 1) save();
            else if (id == 2) state = State.Play;
            else if (id >= 100) {
                if (game.buyPet(id - 100)) {
                    sound.buy();
                    save();
                }
            }
            return true;
        }
        return true;
    }

    private static String petName(int i) {
        String[] names = {"Котёнок", "Щенок", "Кролик", "Хомяк", "Панда", "Лисёнок", "Пингвин", "Дракончик"};
        return names[i % names.length];
    }

    private static String eventName(EventSpawner.Type t) {
        switch (t) {
            case GOLDEN_PAW: return "Золотая лапка!";
            case RAINBOW_BALL: return "Радужный мячик!";
            case NAP_TIME: return "Сладкий сон!";
            default: return "Валежник вкусняшек!";
        }
    }

    private static String format(double v) {
        if (v >= 1e9) return String.format(java.util.Locale.US, "%.2fB", v / 1e9);
        if (v >= 1e6) return String.format(java.util.Locale.US, "%.2fM", v / 1e6);
        if (v >= 1e3) return String.format(java.util.Locale.US, "%.1fK", v / 1e3);
        return String.format(java.util.Locale.US, "%.0f", v);
    }
}
