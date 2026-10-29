package com.farm.game;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

/**
 * «Уютная ферма» — выращивание культур и скрещивание новых сортов.
 * Уютный пастельный стиль: тёплые тона, грядки, цветочки, мягкие тени.
 */
public final class FarmView extends View {

    private enum State { Menu, Play, Breed, Shop }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private State state = State.Play;
    private final List<FarmPlot> plots = new ArrayList<>();
    private final List<String> inventory = new ArrayList<>();
    private int coins = 50;
    private String selectedSeed = "wheat";
    private String breedResult = "";
    private final java.util.Map<String, RectF> buttons = new java.util.LinkedHashMap<>();
    private long lastTick;

    // Уютная палитра
    private final int sky = Color.rgb(250, 246, 233);
    private final int soil = Color.rgb(150, 122, 96);
    private final int grass = Color.rgb(216, 226, 188);
    private final int panel = Color.rgb(255, 250, 240);
    private final int textBrown = Color.rgb(104, 72, 52);
    private final int accent = Color.rgb(240, 160, 90);

    public FarmView(Context context) {
        super(context);
        for (int i = 0; i < 6; i++) plots.add(new FarmPlot(CropCatalog.get("wheat")));
        lastTick = android.os.SystemClock.elapsedRealtime();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = android.os.SystemClock.elapsedRealtime();
        double dt = Math.min((now - lastTick) / 1000.0, 0.1);
        lastTick = now;
        for (FarmPlot p : plots) p.tick(dt);

        canvas.drawColor(sky);
        drawNotebookGrid(canvas);
        drawBackground(canvas);
        buttons.clear();
        switch (state) {
            case Menu -> drawMenu(canvas);
            case Play -> drawPlay(canvas);
            case Breed -> drawBreed(canvas);
            case Shop -> drawShop(canvas);
        }
        postInvalidateDelayed(120);
    }

    /** Клетчатая бумага скетчбука — визуальная подпись игры. */
    private void drawNotebookGrid(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(Color.rgb(205, 215, 228));
        paint.setStrokeWidth(2f);
        float step = w * 0.08f;
        for (float x = 0; x < w; x += step) canvas.drawLine(x, 0, x, h, paint);
        for (float y = 0; y < h; y += step) canvas.drawLine(0, y, w, y, paint);
        // поля и «скотч»
        paint.setColor(Color.rgb(238, 190, 190));
        paint.setStrokeWidth(5f);
        canvas.drawLine(w * 0.1f, 0, w * 0.1f, h, paint);
    }

    private void drawBackground(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(grass);
        canvas.drawRect(0, h * 0.55f, w, h, paint);
        // цветочки
        paint.setColor(Color.rgb(238, 190, 190));
        for (int i = 0; i < 6; i++) {
            canvas.drawCircle(w * (0.08f + i * 0.17f), h * 0.62f, w * 0.018f, paint);
            canvas.drawCircle(w * (0.12f + i * 0.17f), h * 0.66f, w * 0.015f, paint);
        }
    }

    private void drawPlay(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(textBrown);
        paint.setTextSize(w * 0.055f);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Монеты: " + coins, w * 0.05f, w * 0.1f, paint);
        paint.setTextSize(w * 0.036f);
        canvas.drawText("семя: " + selectedSeed, w * 0.05f, w * 0.15f, paint);

        // Грядки 3x2
        for (int i = 0; i < plots.size(); i++) {
            int col = i % 3;
            int row = i / 3;
            float x = w * 0.06f + col * w * 0.31f;
            float y = h * 0.24f + row * h * 0.17f;
            RectF r = new RectF(x, y, x + w * 0.28f, y + h * 0.13f);
            buttons.put("plot" + i, r);
            paint.setColor(soil);
            canvas.drawRoundRect(r, 22, 22, paint);
            // росток
            FarmPlot plot = plots.get(i);
            float growth = (float) plot.growth();
            paint.setColor(growth >= 1f ? Color.rgb(242, 190, 90) : Color.rgb(120, 180, 100));
            float sprout = w * 0.03f + growth * w * 0.05f;
            canvas.drawCircle(x + w * 0.14f, y + h * 0.07f, sprout, paint);
            if (plot.isRipe()) {
                paint.setColor(Color.rgb(255, 240, 200));
                paint.setTextSize(w * 0.03f);
                paint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText("собрать!", x + w * 0.14f, y + h * 0.115f, paint);
            }
        }

        button(canvas, "breed", "Скрещивание", w * 0.27f, h * 0.62f, w * 0.42f, h * 0.065f);
        button(canvas, "shop", "Магазин семян", w * 0.73f, h * 0.62f, w * 0.42f, h * 0.065f);

        // Инвентарь
        paint.setColor(panel);
        canvas.drawRoundRect(new RectF(w * 0.05f, h * 0.72f, w * 0.95f, h * 0.95f), 24, 24, paint);
        paint.setColor(textBrown);
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(w * 0.034f);
        canvas.drawText("Собранные культуры:", w * 0.08f, h * 0.76f, paint);
        float ix = w * 0.08f;
        float iy = h * 0.81f;
        for (String item : inventory) {
            canvas.drawText(item, ix, iy, paint);
            iy += w * 0.05f;
            if (iy > h * 0.93f) {
                iy = h * 0.81f;
                ix += w * 0.3f;
            }
        }
    }

    private void drawBreed(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(textBrown);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.06f);
        canvas.drawText("Скрещивание", w / 2f, h * 0.22f, paint);
        paint.setTextSize(w * 0.038f);
        canvas.drawText("выбери два сорта — получи новый!", w / 2f, h * 0.28f, paint);

        List<String> names = CropCatalog.names();
        for (int i = 0; i < Math.min(6, names.size()); i++) {
            int col = i % 3;
            int row = i / 3;
            float x = w * 0.08f + col * w * 0.3f;
            float y = h * 0.34f + row * h * 0.1f;
            button(canvas, 200 + i, names.get(i), x + w * 0.13f, y + h * 0.035f, w * 0.26f, h * 0.06f);
        }
        if (!breedResult.isEmpty()) {
            paint.setColor(accent);
            paint.setTextSize(w * 0.05f);
            canvas.drawText("Результат: " + breedResult, w / 2f, h * 0.62f, paint);
        }
        button(canvas, "back", "Назад", w / 2f, h * 0.72f, w * 0.5f, h * 0.065f);
    }

    private void drawShop(Canvas canvas) {
        float w = getWidth();
        float h = getHeight();
        paint.setColor(textBrown);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(w * 0.06f);
        canvas.drawText("Магазин семян", w / 2f, h * 0.22f, paint);
        List<String> names = CropCatalog.names();
        for (int i = 0; i < Math.min(6, names.size()); i++) {
            String name = names.get(i);
            Crop crop = CropCatalog.get(name);
            int col = i % 3;
            int row = i / 3;
            float x = w * 0.08f + col * w * 0.3f;
            float y = h * 0.34f + row * h * 0.1f;
            button(canvas, 300 + i, name + " (" + crop.sellPrice + "м)", x + w * 0.13f, y + h * 0.035f, w * 0.26f, h * 0.06f);
        }
        button(canvas, "back", "Назад", w / 2f, h * 0.72f, w * 0.5f, h * 0.065f);
    }

    private void drawMenu(Canvas canvas) {
    }

    private void button(Canvas canvas, int id, String label, float cx, float cy, float bw, float bh) {
        button(canvas, String.valueOf(id), label, cx, cy, bw, bh);
    }

    private void button(Canvas canvas, String name, String label, float cx, float cy, float bw, float bh) {
        RectF r = new RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2);
        buttons.put(name, r);
        paint.setColor(accent);
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint);
        paint.setColor(Color.WHITE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bh * 0.4f);
        canvas.drawText(label, cx, cy + bh * 0.14f, paint);
    }

    private String breedParentA = "";

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() != MotionEvent.ACTION_DOWN) return true;
        float x = event.getX();
        float y = event.getY();

        for (java.util.Map.Entry<String, RectF> e : buttons.entrySet()) {
            if (!e.getValue().contains(x, y)) continue;
            String name = e.getKey();
            if (name.startsWith("plot")) {
                int idx = Integer.parseInt(name.substring(4));
                FarmPlot plot = plots.get(idx);
                if (plot.isRipe()) {
                    int yield = plot.harvest();
                    for (int i = 0; i < yield; i++) inventory.add(plot.crop().name);
                    coins += yield * plot.crop().sellPrice;
                    plots.set(idx, new FarmPlot(CropCatalog.get(selectedSeed)));
                } else {
                    plot.water();
                }
            } else if (name.equals("breed")) {
                state = State.Breed;
            } else if (name.equals("shop")) {
                state = State.Shop;
            } else if (name.equals("back")) {
                state = State.Play;
            } else if (name.matches("\\d+")) {
                int id = Integer.parseInt(name);
                if (id >= 200 && id < 300) {
                    String cropName = CropCatalog.names().get(id - 200);
                    if (breedParentA.isEmpty()) {
                        breedParentA = cropName;
                        breedResult = "выбран 1-й родитель: " + cropName;
                    } else {
                        breedResult = BreedingBook.breed(breedParentA, cropName, System.nanoTime());
                        breedParentA = "";
                    }
                } else if (id >= 300) {
                    String cropName = CropCatalog.names().get(id - 300);
                    Crop crop = CropCatalog.get(cropName);
                    if (coins >= crop.sellPrice * 2) {
                        coins -= crop.sellPrice * 2;
                        selectedSeed = cropName;
                    }
                }
            }
            invalidate();
            return true;
        }
        return true;
    }
}
