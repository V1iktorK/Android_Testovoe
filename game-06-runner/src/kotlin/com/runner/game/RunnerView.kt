package com.runner.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.min

/**
 * «Мультяшный раннер» — свайпы: влево/вправо — смена полосы, вверх — прыжок, вниз — кувырок.
 * Яркий мультяшный стиль: градиент неба, холмы, облака, цветные препятствия.
 */
class RunnerView(context: Context) : View(context) {

    private enum class State { Menu, HeroSelect, Play, GameOver }

    private var state = State.Menu
    private val lane = LaneState()
    private var track = TrackGenerator.generate(20261028, 400)
    private var distanceCells = 0       // пройдено клеток трассы
    private var subCell = 0f            // плавное смещение внутри клетки
    private var coins = 0
    private var bestDistance = 0
    private var selectedHero = 1
    private var jumpTimer = 0f
    private var rollTimer = 0f
    private var hitFlash = 0f
    private val sound = RunnerSound()
    private var lastTime = SystemClock.elapsedRealtime()
    private var downX = 0f
    private var downY = 0f
    private var swiped = false

    // --- Палитра (мультяшная) ---
    private val skyTop = Color.rgb(116, 192, 252)
    private val skyBottom = Color.rgb(201, 240, 255)
    private val hillFar = Color.rgb(120, 200, 130)
    private val hillNear = Color.rgb(86, 175, 105)
    private val road = Color.rgb(238, 226, 200)
    private val textColor = Color.rgb(45, 60, 85)
    private val heroColors = intArrayOf(Color.rgb(255, 122, 89), Color.rgb(96, 176, 255), Color.rgb(178, 102, 255))
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttons = mutableMapOf<String, RectF>()

    init {
        loadBest()
    }

    private fun loadBest() {
        val prefs = context.getSharedPreferences("runner", Context.MODE_PRIVATE)
        bestDistance = prefs.getInt("best", 0)
        selectedHero = prefs.getInt("hero", 1)
    }

    private fun saveBest() {
        val prefs = context.getSharedPreferences("runner", Context.MODE_PRIVATE)
        val editor = prefs.edit()
        if (distanceCells > bestDistance) {
            editor.putInt("best", distanceCells)
            if (distanceCells >= 2000 && distanceCells < 5000) sound.record()
            bestDistance = distanceCells
        }
        editor.putInt("hero", selectedHero)
        editor.apply()
    }

    // ================== Игровой цикл ==================

    private fun startRun() {
        track = TrackGenerator.generate(System.nanoTime(), 4000)
        distanceCells = 0
        subCell = 0f
        coins = 0
        lane.settle()
        jumpTimer = 0f
        rollTimer = 0f
        state = State.Play
    }

    override fun onDraw(canvas: Canvas) {
        val now = SystemClock.elapsedRealtime()
        val dt = min((now - lastTime) / 1000f, 0.05f)
        lastTime = now

        drawBackground(canvas)

        if (state == State.Play) {
            step(dt)
            drawWorld(canvas)
            drawHud(canvas)
        } else {
            drawWorld(canvas)
            when (state) {
                State.Menu -> drawMenu(canvas)
                State.HeroSelect -> drawHeroSelect(canvas)
                State.GameOver -> drawGameOver(canvas)
                else -> {}
            }
        }
        if (hitFlash > 0f) {
            paint.color = Color.argb((hitFlash * 120).toInt(), 255, 60, 60)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            hitFlash -= dt * 2f
        }
        postInvalidateDelayed(30)
    }

    private fun step(dt: Float) {
        val speed = RunRules.speedAt(distanceCells) // клеток в секунду
        subCell += speed * dt
        while (subCell >= 1f) {
            subCell -= 1f
            distanceCells++
            checkCell()
        }
        if (jumpTimer > 0f) {
            jumpTimer -= dt
            if (jumpTimer <= 0f) lane.settle()
        }
        if (rollTimer > 0f) {
            rollTimer -= dt
            if (rollTimer <= 0f) lane.settle()
        }
    }

    private fun checkCell() {
        val cell = track.getOrElse(distanceCells) { '.' }
        when (cell) {
            'b', 'u' -> {
                if (cell == 'b' || lane.isJumping) {
                    coins++
                    sound.coin()
                }
            }
            'x', 'h', 'l' -> {
                val hit = when (cell) {
                    'x' -> true
                    'h' -> !lane.isJumping
                    else -> !lane.isRolling
                }
                if (hit) {
                    sound.hit()
                    hitFlash = 1f
                    saveBest()
                    state = State.GameOver
                }
            }
        }
    }

    // ================== Отрисовка ==================

    private fun drawBackground(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        for (i in 0 until 12) {
            val t = i / 11f
            paint.color = blend(skyTop, skyBottom, t)
            canvas.drawRect(0f, t * h * 0.55f, w, (t + 1f) * h * 0.55f, paint)
        }
        // облака
        paint.color = Color.WHITE
        val cloudShift = (distanceCells * 12) % (w + 400)
        for (i in 0 until 3) {
            val cx = w - ((cloudShift + i * w * 0.55f) % (w + 400f)) + 200f
            canvas.drawCircle(cx, h * (0.12f + i * 0.07f), w * 0.09f, paint)
            canvas.drawCircle(cx + w * 0.08f, h * (0.12f + i * 0.07f), w * 0.07f, paint)
        }
        // холмы
        paint.color = hillFar
        canvas.drawCircle(w * 0.2f, h * 0.62f, w * 0.35f, paint)
        canvas.drawCircle(w * 0.85f, h * 0.62f, w * 0.3f, paint)
        paint.color = hillNear
        canvas.drawCircle(w * 0.5f, h * 0.68f, w * 0.42f, paint)
        // дорога
        paint.color = road
        canvas.drawRect(0f, h * 0.55f, w, h, paint)
    }

    private fun drawWorld(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val roadTop = h * 0.58f
        val roadBottom = h * 0.97f
        val laneWidth = w / 3f

        // разметка полос
        paint.color = Color.argb(60, 255, 255, 255)
        paint.strokeWidth = 6f
        for (i in 1..2) {
            canvas.drawLine(i * laneWidth, roadTop, i * laneWidth, roadBottom, paint)
        }

        // препятствия: рисуем впереди на 5 клеток
        val cellHeight = (roadBottom - roadTop) / 5f
        for (ahead in 1..5) {
            val idx = distanceCells + ahead
            val cell = track.getOrElse(idx) { '.' }
            if (cell == '.') continue
            val y = roadBottom - (ahead - subCell) * cellHeight - cellHeight
            val cx = laneWidth * 1.5f // препятствия рисуем по центральной полосе-маршруту
            drawObstacle(canvas, cell, cx - laneWidth * 0.35f, y, laneWidth * 0.7f, cellHeight * 0.85f)
        }

        // герой в своей полосе
        val heroX = laneWidth * (lane.current + 0.5f)
        val heroY = roadBottom - cellHeight * 0.9f - if (lane.isJumping) cellHeight * 0.7f else 0f
        drawHero(canvas, heroX, heroY, laneWidth * 0.42f)
    }

    private fun drawObstacle(canvas: Canvas, kind: Char, x: Float, y: Float, w: Float, h: Float) {
        when (kind) {
            'x' -> {
                paint.color = Color.rgb(214, 74, 74)
                canvas.drawRoundRect(RectF(x, y, x + w, y + h), 18f, 18f, paint)
                paint.color = Color.rgb(150, 40, 40)
                canvas.drawRect(x + w * 0.15f, y + h * 0.35f, x + w * 0.85f, y + h * 0.55f, paint)
            }
            'h' -> {
                paint.color = Color.rgb(255, 172, 64)
                canvas.drawRoundRect(RectF(x, y + h * 0.35f, x + w, y + h), 14f, 14f, paint)
            }
            'l' -> {
                paint.color = Color.rgb(88, 132, 222)
                paint.strokeWidth = 14f
                canvas.drawLine(x + w * 0.1f, y + h * 0.3f, x + w * 0.9f, y + h * 0.3f, paint)
                canvas.drawLine(x + w * 0.1f, y + h * 0.7f, x + w * 0.9f, y + h * 0.7f, paint)
            }
            'b' -> drawCoin(canvas, x + w / 2, y + h / 2, w * 0.22f)
            'u' -> drawCoin(canvas, x + w / 2, y + h * 0.15f, w * 0.22f)
        }
    }

    private fun drawCoin(canvas: Canvas, cx: Float, cy: Float, r: Float) {
        paint.color = Color.rgb(255, 206, 68)
        canvas.drawCircle(cx, cy, r, paint)
        paint.color = Color.rgb(216, 160, 32)
        canvas.drawCircle(cx, cy, r * 0.55f, paint)
    }

    private fun drawHero(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        val color = heroColors[(selectedHero - 1).coerceIn(0, 2)]
        val bodyH = if (lane.isRolling) size * 0.7f else size * 1.15f
        paint.color = color
        canvas.drawRoundRect(RectF(cx - size * 0.32f, cy - bodyH, cx + size * 0.32f, cy), size * 0.28f, size * 0.28f, paint)
        paint.color = Color.rgb(255, 226, 190)
        canvas.drawCircle(cx, cy - bodyH - size * 0.18f, size * 0.3f, paint)
        paint.color = textColor
        canvas.drawCircle(cx - size * 0.1f, cy - bodyH - size * 0.22f, size * 0.05f, paint)
        canvas.drawCircle(cx + size * 0.1f, cy - bodyH - size * 0.22f, size * 0.05f, paint)
    }

    private fun drawHud(canvas: Canvas) {
        val w = width.toFloat()
        paint.color = textColor
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = w * 0.05f
        canvas.drawText("Дистанция: $distanceCells м", w * 0.04f, w * 0.1f, paint)
        paint.textSize = w * 0.042f
        canvas.drawText("Монеты: $coins", w * 0.04f, w * 0.155f, paint)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Рекорд: $bestDistance", w * 0.96f, w * 0.1f, paint)
    }

    private fun drawMenu(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = textColor
        paint.textSize = w * 0.11f
        canvas.drawText("МУЛЬТЯШНЫЙ РАННЕР", w / 2, h * 0.3f, paint)
        paint.textSize = w * 0.045f
        canvas.drawText("свайпы: ◄ ► полосы, ▲ прыжок, ▼ кувырок", w / 2, h * 0.36f, paint)
        button(canvas, "play", "Бежать!", w / 2, h * 0.52f, w * 0.55f, h * 0.075f)
        button(canvas, "heroes", "Герои", w / 2, h * 0.63f, w * 0.55f, h * 0.075f)
        paint.textSize = w * 0.04f
        paint.color = textColor
        canvas.drawText("рекорд: $bestDistance м", w / 2, h * 0.73f, paint)
    }

    private fun drawHeroSelect(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = textColor
        paint.textSize = w * 0.08f
        canvas.drawText("Выбор героя", w / 2, h * 0.22f, paint)
        for (i in 1..3) {
            val unlocked = RunRules.isUnlocked(i, bestDistance)
            val cx = w * (0.2f + 0.3f * (i - 1))
            val cy = h * 0.45f
            drawHero(canvas, cx, cy + w * 0.12f, w * 0.12f)
            paint.textSize = w * 0.035f
            val need = when (i) { 2 -> 2000; 3 -> 5000; else -> 0 }
            paint.color = if (unlocked) textColor else Color.rgb(150, 150, 160)
            canvas.drawText(if (unlocked) "герой $i" else "откроется на $need м", cx, cy + w * 0.3f, paint)
            if (unlocked) {
                button(canvas, "hero$i", if (i == selectedHero) "Выбран" else "Выбрать", cx, cy + w * 0.42f, w * 0.26f, h * 0.05f)
            }
        }
        button(canvas, "back", "Назад", w / 2, h * 0.85f, w * 0.5f, h * 0.065f)
    }

    private fun drawGameOver(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = textColor
        paint.textSize = w * 0.1f
        canvas.drawText("Финиш!", w / 2, h * 0.3f, paint)
        paint.textSize = w * 0.05f
        canvas.drawText("дистанция: $distanceCells м   монеты: $coins", w / 2, h * 0.37f, paint)
        if (distanceCells >= bestDistance && distanceCells > 0) {
            paint.color = Color.rgb(230, 120, 30)
            canvas.drawText("новый рекорд!", w / 2, h * 0.43f, paint)
        }
        button(canvas, "again", "Ещё раз", w / 2, h * 0.55f, w * 0.55f, h * 0.075f)
        button(canvas, "back", "В меню", w / 2, h * 0.66f, w * 0.55f, h * 0.075f)
    }

    private fun button(canvas: Canvas, name: String, label: String, cx: Float, cy: Float, bw: Float, bh: Float) {
        val r = RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2)
        buttons[name] = r
        paint.color = Color.rgb(255, 138, 96)
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint)
        paint.color = Color.WHITE
        paint.textSize = bh * 0.42f
        canvas.drawText(label, cx, cy + bh * 0.15f, paint)
    }

    // ================== Касания и свайпы ==================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                swiped = false
            }
            MotionEvent.ACTION_MOVE -> {
                if (!swiped && state == State.Play) {
                    val dx = event.x - downX
                    val dy = event.y - downY
                    if (abs(dx) > width * 0.08f) {
                        if (dx > 0) lane.swipeRight() else lane.swipeLeft()
                        swiped = true
                    } else if (abs(dy) > height * 0.06f) {
                        if (dy < 0) {
                            lane.swipeUp()
                            jumpTimer = 0.65f
                            sound.jump()
                        } else {
                            lane.swipeDown()
                            rollTimer = 0.65f
                        }
                        swiped = true
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                if (!swiped) handleTap(event.x, event.y)
            }
        }
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        for ((name, r) in buttons) {
            if (!r.contains(x, y)) continue
            when (name) {
                "play", "again" -> startRun()
                "heroes" -> state = State.HeroSelect
                "back" -> state = State.Menu
                "hero1" -> { selectedHero = 1; saveBest() }
                "hero2" -> { selectedHero = 2; saveBest() }
                "hero3" -> { selectedHero = 3; saveBest() }
            }
            return
        }
    }

    private fun blend(a: Int, b: Int, t: Float): Int {
        val ar = Color.red(a); val ag = Color.green(a); val ab = Color.blue(a)
        val br = Color.red(b); val bg = Color.green(b); val bb = Color.blue(b)
        return Color.rgb(
            (ar + (br - ar) * t).toInt(),
            (ag + (bg - ag) * t).toInt(),
            (ab + (bb - ab) * t).toInt()
        )
    }
}
