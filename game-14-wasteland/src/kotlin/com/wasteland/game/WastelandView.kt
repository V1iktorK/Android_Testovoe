package com.wasteland.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View

/**
 * «Пепел» — выживание в постапокалипсисе: поиск ресурсов, крафт, день/ночь.
 * Стиль: выцветшие песочно-серые тона, оранжевые закатные акценты, ретро-радио-шум.
 */
class WastelandView(context: Context) : View(context) {

    private enum class State { Menu, Play, Craft, GameOver }

    private val survival = SurvivalState()
    private val book = CraftingBook()
    private val inventory = HashMap<String, Int>()
    private var state = State.Menu
    private var day = 1
    private var isDay = true
    private var actionsToday = 0
    private var eventText = ""
    private var eventTime = 0f
    private var lastTime = SystemClock.elapsedRealtime()
    private val buttons = ArrayList<RectF>()
    private val buttonIds = ArrayList<Int>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val prefs = context.getSharedPreferences("wasteland", Context.MODE_PRIVATE)

    // Палитра пустоши
    private val dayBg = Color.rgb(196, 178, 146)
    private val nightBg = Color.rgb(48, 50, 66)
    private val panel = Color.rgb(62, 56, 48)
    private val accent = Color.rgb(217, 120, 68)
    private val textLight = Color.rgb(242, 236, 222)
    private val danger = Color.rgb(178, 64, 54)

    // ================== Игровой цикл ==================

    private fun startGame() {
        day = 1
        isDay = true
        actionsToday = 0
        inventory.clear()
        inventory["wood"] = 2
        inventory["water"] = 1
        eventText = "Ты очнулся в пепле. Найди ресурсы до ночи."
        eventTime = 4f
        state = State.Play
    }

    /** Действие: обыск локации. Тратит время суток. */
    private fun scavenge(spot: String) {
        val loot = LootTable.generate(20261028L + day, spot, day)
        val sb = StringBuilder("Добыча: ")
        for (drop in loot) {
            inventory[drop.item] = (inventory[drop.item] ?: 0) + 1
            sb.append(drop.item).append(" ")
        }
        eventText = sb.toString()
        eventTime = 3.5f
        advanceTime()
    }

    private fun advanceTime() {
        actionsToday++
        survival.tick(isDay)
        if (actionsToday >= 3) {
            actionsToday = 0
            isDay = !isDay
            if (isDay) {
                day++
                if (day > prefs.getInt("best", 0)) prefs.edit().putInt("best", day).apply()
            }
        }
        if (!survival.isAlive()) state = State.GameOver
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        val dt = minOf((now - lastTime) / 1000f, 0.1f)
        lastTime = now
        if (eventTime > 0f) eventTime -= dt

        canvas.drawColor(if (isDay) dayBg else nightBg)
        buttons.clear()
        buttonIds.clear()

        drawHeader(canvas)
        when (state) {
            State.Menu -> drawMenu(canvas)
            State.Play -> drawPlay(canvas)
            State.Craft -> drawCraft(canvas)
            State.GameOver -> drawGameOver(canvas)
        }
        if (eventTime > 0f) drawEvent(canvas)
        postInvalidateDelayed(100)
    }

    private fun drawHeader(canvas: Canvas) {
        val w = width.toFloat()
        paint.color = if (isDay) panel else Color.rgb(34, 34, 48)
        canvas.drawRect(0f, 0f, w, height * 0.16f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.color = textLight
        paint.textSize = w * 0.045f
        canvas.drawText("День $day · ${if (isDay) "день" else "ночь"}", w * 0.04f, w * 0.07f, paint)
        drawBar(canvas, w * 0.04f, w * 0.11f, w * 0.28f, survival.health.toFloat() / 100f, Color.rgb(120, 200, 120), "ЗДОРОВЬЕ")
        drawBar(canvas, w * 0.36f, w * 0.11f, w * 0.28f, 1f - survival.hunger.toFloat() / 100f, accent, "ГОЛОД")
        drawBar(canvas, w * 0.68f, w * 0.11f, w * 0.28f, survival.warmth.toFloat() / 100f, Color.rgb(120, 170, 230), "ТЕПЛО")
    }

    private fun drawBar(canvas: Canvas, x: Float, y: Float, w: Float, frac: Float, color: Int, label: String) {
        paint.color = Color.argb(80, 0, 0, 0)
        canvas.drawRect(x, y, x + w, y + w * 0.12f, paint)
        paint.color = color
        canvas.drawRect(x, y, x + w * frac.coerceIn(0f, 1f), y + w * 0.12f, paint)
        paint.color = textLight
        paint.textSize = w * 0.1f
        canvas.drawText(label, x, y + w * 0.3f, paint)
    }

    private fun drawPlay(canvas: Canvas) {
        // сигнатура: зелёная шкала радиационного фона
        paint.color = Color.rgb(110, 220, 110)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = width * 0.026f
        canvas.drawText("радиационный фон: ${day * 3} мкР/ч", width * 0.04f, height * 0.235f, paint)
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = textLight
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = w * 0.05f
        canvas.drawText("Обыск местности", w / 2f, h * 0.24f, paint)

        val spots = listOf("street" to "Улица", "hospital" to "Больница", "factory" to "Завод", "military" to "База")
        for ((i, spot) in spots.withIndex()) {
            val x = w * 0.06f + (i % 2) * w * 0.47f
            val y = h * 0.29f + (i / 2) * h * 0.13f
            button(canvas, 100 + i, "Обыскать: ${spot.second}", x + w * 0.21f, y + h * 0.05f, w * 0.42f, h * 0.085f)
        }
        button(canvas, 2, "Крафт", w * 0.27f, h * 0.62f, w * 0.42f, h * 0.07f)
        button(canvas, 3, "Поесть (еда)", w * 0.73f, h * 0.62f, w * 0.42f, h * 0.07f)
        button(canvas, 4, "Костёр (дрова)", w * 0.27f, h * 0.73f, w * 0.42f, h * 0.07f)
        button(canvas, 5, "Отдых", w * 0.73f, h * 0.73f, w * 0.42f, h * 0.07f)

        // Инвентарь
        paint.color = panel
        canvas.drawRoundRect(RectF(w * 0.05f, h * 0.82f, w * 0.95f, h * 0.97f), 20f, 20f, paint)
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = w * 0.032f
        var ix = w * 0.08f
        var iy = h * 0.86f
        for ((item, count) in inventory.toSortedMap()) {
            if (count <= 0) continue
            canvas.drawText("$item ×$count", ix, iy, paint)
            iy += w * 0.05f
            if (iy > h * 0.95f) {
                iy = h * 0.86f
                ix += w * 0.3f
            }
        }
    }

    private fun drawCraft(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = textLight
        paint.textSize = w * 0.055f
        canvas.drawText("Крафт", w / 2f, h * 0.24f, paint)
        val items = book.all().toList()
        for ((i, item) in items.withIndex()) {
            val x = w * 0.06f + (i % 2) * w * 0.47f
            val y = h * 0.29f + (i / 2) * h * 0.11f
            val ing = book.ingredientsFor(item).entries.joinToString(", ") { "${it.key}×${it.value}" }
            button(canvas, 300 + i, "$item ($ing)", x + w * 0.22f, y + h * 0.038f, w * 0.44f, h * 0.068f)
        }
        button(canvas, 6, "Назад", w / 2f, h * 0.86f, w * 0.5f, h * 0.065f)
    }

    private fun drawMenu(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = Color.rgb(242, 236, 222)
        paint.textSize = w * 0.12f
        canvas.drawText("ПЕПЕЛ", w / 2f, h * 0.32f, paint)
        paint.textSize = w * 0.042f
        canvas.drawText("выживание в пустоши: ресурсы, крафт, день и ночь", w / 2f, h * 0.39f, paint)
        button(canvas, 1, "Начать", w / 2f, h * 0.55f, w * 0.55f, h * 0.075f)
        paint.color = Color.rgb(230, 220, 200)
        paint.textSize = w * 0.038f
        canvas.drawText("лучший день: ${prefs.getInt("best", 0)}", w / 2f, h * 0.66f, paint)
    }

    private fun drawGameOver(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.color = danger
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = w * 0.1f
        canvas.drawText("Ты не выжил", w / 2f, h * 0.35f, paint)
        paint.color = textLight
        paint.textSize = w * 0.05f
        canvas.drawText("прожито дней: $day   очки: ${SurvivalState.scoreFor(day)}", w / 2f, h * 0.43f, paint)
        button(canvas, 1, "Ещё раз", w / 2f, h * 0.56f, w * 0.55f, h * 0.075f)
    }

    private fun drawEvent(canvas: Canvas) {
        val w = width.toFloat()
        paint.color = Color.argb(210, 40, 36, 30)
        canvas.drawRoundRect(RectF(w * 0.06f, height * 0.17f, w * 0.94f, height * 0.235f), 18f, 18f, paint)
        paint.color = textLight
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = w * 0.036f
        canvas.drawText(eventText, w / 2f, height * 0.212f, paint)
    }

    private fun button(canvas: Canvas, id: Int, label: String, cx: Float, cy: Float, bw: Float, bh: Float) {
        val r = RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2)
        buttons.add(r)
        buttonIds.add(id)
        paint.color = if (id == 1) accent else panel
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint)
        paint.color = textLight
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = minOf(bh * 0.4f, bw * 0.09f)
        canvas.drawText(label, cx, cy + bh * 0.14f, paint)
    }

    // ================== Касания ==================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        val x = event.x
        val y = event.y
        for (i in buttons.indices) {
            if (!buttons[i].contains(x, y)) continue
            when (val id = buttonIds[i]) {
                1 -> startGame()
                2 -> state = State.Craft
                3 -> {
                    if ((inventory["canned_food"] ?: 0) > 0) {
                        inventory["canned_food"] = inventory["canned_food"]!! - 1
                        survival.eat(35.0)
                        eventText = "Ты съел консерву"
                        eventTime = 2.5f
                        advanceTime()
                    } else {
                        eventText = "Нет еды!"
                        eventTime = 2f
                    }
                }
                4 -> {
                    if ((inventory["wood"] ?: 0) >= 3) {
                        inventory["wood"] = inventory["wood"]!! - 3
                        survival.warmUp(45.0)
                        eventText = "Костёр согрел тебя"
                        eventTime = 2.5f
                        advanceTime()
                    } else {
                        eventText = "Нужно 3 дров"
                        eventTime = 2f
                    }
                }
                5 -> {
                    survival.warmUp(10.0)
                    eventText = "Ты отдохнул"
                    eventTime = 2f
                    advanceTime()
                }
                6 -> state = State.Play
                in 100..103 -> {
                    val spots = listOf("street", "hospital", "factory", "military")
                    scavenge(spots[id - 100])
                }
                in 300..399 -> {
                    val item = book.all().toList()[id - 300]
                    if (book.craft(inventory, item)) {
                        eventText = "Создано: $item"
                        eventTime = 2.5f
                    } else {
                        eventText = "Не хватает ресурсов"
                        eventTime = 2f
                    }
                }
            }
            invalidate()
            return true
        }
        return true
    }
}
