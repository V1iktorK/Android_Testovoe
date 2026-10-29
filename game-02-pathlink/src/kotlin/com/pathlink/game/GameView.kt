package com.pathlink.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * Игровой экран «Соедини пути»: меню, выбор уровня, головоломка, победа.
 * Минималистичный пастельный стиль — рисование примитивами Canvas.
 */
class GameView(context: Context) : View(context) {

    private enum class State { MENU, LEVELS, PLAY, WIN }

    private var state = State.MENU
    private var currentLevel = 1
    private lateinit var board: Board
    private var par = 0
    private var hintsLeft = 3
    private var lastStars = 0
    private val locked = mutableSetOf<Int>()
    private val tone = TonePlayer()
    private val progress: Progress = loadProgress()

    // --- Палитра (пастель) ---
    private val bg = Color.rgb(238, 229, 206)
    private val tileFill = Color.rgb(212, 202, 182)
    private val pipeColor = Color.rgb(116, 150, 104)
    private val pipeActive = Color.rgb(196, 120, 86)
    private val textColor = Color.rgb(88, 70, 52)
    private val lockedColor = Color.rgb(170, 158, 138)
    private val starColor = Color.rgb(240, 190, 90)

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonRects = mutableMapOf<String, RectF>()
    private var boardRect = RectF()
    private var cellSize = 0f

    // ================== Сохранение ==================

    private fun loadProgress(): Progress {
        val prefs = context.getSharedPreferences("pathlink", Context.MODE_PRIVATE)
        return Progress.deserialize(prefs.getString("progress", "") ?: "")
    }

    fun saveProgress() {
        context.getSharedPreferences("pathlink", Context.MODE_PRIVATE)
            .edit().putString("progress", progress.serialize()).apply()
    }

    // ================== Запуск уровня ==================

    private fun startLevel(level: Int) {
        currentLevel = level
        val params = LevelCatalog.paramsFor(level)
        board = BoardGenerator.generate(params.boardSize, params.seed)
        par = board.parMoves()
        hintsLeft = 3
        locked.clear()
        // Часть плиток закреплена в решении — зависит от сложности
        val rng = java.util.Random(params.seed xor 0x5F5F)
        repeat(params.difficulty - 1) {
            val idx = rng.nextInt(params.boardSize * params.boardSize)
            board.applyHint(idx)
            locked.add(idx)
        }
        state = State.PLAY
        invalidate()
    }

    // ================== Касания ==================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked != MotionEvent.ACTION_DOWN) return true
        val x = event.x
        val y = event.y
        when (state) {
            State.MENU -> {
                if (hit("play", x, y)) { state = State.LEVELS; invalidate() }
            }
            State.LEVELS -> {
                for ((key, r) in buttonRects) {
                    if (r.contains(x, y) && key.startsWith("lvl")) {
                        val lvl = key.removePrefix("lvl").toInt()
                        if (progress.isUnlocked(lvl)) startLevel(lvl)
                        return true
                    }
                }
                if (hit("back", x, y)) { state = State.MENU; invalidate() }
            }
            State.PLAY -> {
                if (hit("back", x, y)) { state = State.LEVELS; invalidate(); return true }
                if (hit("hint", x, y)) { useHint(); return true }
                if (boardRect.contains(x, y)) {
                    val cx = ((x - boardRect.left) / cellSize).toInt()
                    val cy = ((y - boardRect.top) / cellSize).toInt()
                    val idx = cy * board.size + cx
                    if (cx in 0 until board.size && cy in 0 until board.size && idx !in locked) {
                        board.rotate(cx, cy)
                        tone.click()
                        if (board.isSolved()) {
                            lastStars = Progress.starsFor(board.moveCount(), par)
                            progress.complete(currentLevel, lastStars, board.moveCount())
                            saveProgress()
                            tone.win()
                            state = State.WIN
                        }
                        invalidate()
                    }
                }
            }
            State.WIN -> {
                if (hit("next", x, y)) {
                    if (currentLevel < LevelCatalog.LEVEL_COUNT) startLevel(currentLevel + 1)
                    else { state = State.LEVELS; invalidate() }
                }
                if (hit("back", x, y)) { state = State.LEVELS; invalidate() }
            }
        }
        return true
    }

    private fun hit(name: String, x: Float, y: Float): Boolean =
        buttonRects[name]?.contains(x, y) == true

    private fun useHint() {
        val idx = board.firstUnsolvedTile()
        if (idx >= 0 && hintsLeft > 0) {
            board.applyHint(idx)
            hintsLeft--
            tone.hint()
            if (board.isSolved()) {
                lastStars = Progress.starsFor(board.moveCount(), par)
                progress.complete(currentLevel, lastStars, board.moveCount())
                saveProgress()
                tone.win()
                state = State.WIN
            }
            invalidate()
        }
    }

    // ================== Отрисовка ==================

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(bg)
        buttonRects.clear()
        when (state) {
            State.MENU -> drawMenu(canvas)
            State.LEVELS -> drawLevels(canvas)
            State.PLAY -> drawPlay(canvas)
            State.WIN -> { drawPlay(canvas, dimmed = true); drawWin(canvas) }
        }
    }

    private fun text(canvas: Canvas, s: String, x: Float, y: Float, size: Float, align: Paint.Align = Paint.Align.CENTER, color: Int = textColor) {
        paint.color = color
        paint.textAlign = align
        paint.textSize = size
        paint.isFakeBoldText = false
        canvas.drawText(s, x, y, paint)
    }

    private fun button(canvas: Canvas, name: String, label: String, cx: Float, cy: Float, w: Float, h: Float) {
        val r = RectF(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2)
        buttonRects[name] = r
        paint.color = pipeColor
        canvas.drawRoundRect(r, h / 2, h / 2, paint)
        text(canvas, label, cx, cy + h * 0.18f, h * 0.42f, color = Color.WHITE)
    }

    private fun drawMenu(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        text(canvas, "Соедини пути", w / 2, h * 0.30f, w * 0.11f)
        text(canvas, "поверни плитки и замкни сеть", w / 2, h * 0.36f, w * 0.045f)
        // Декоративная плитка
        val demo = h * 0.11f
        drawTile(canvas, w / 2 - demo / 2, h * 0.46f, demo, Tile(false, true, true, true), pipeActive, 0)
        button(canvas, "play", "Играть", w / 2, h * 0.70f, w * 0.52f, h * 0.075f)
        text(canvas, "всего звёзд: ${progress.totalStars()} / ${LevelCatalog.LEVEL_COUNT * 3}", w / 2, h * 0.80f, w * 0.04f)
    }

    private fun drawLevels(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        text(canvas, "Уровни", w / 2, h * 0.08f, w * 0.08f)
        val cols = 6
        val rows = 10
        val gridW = w * 0.88f
        val cell = gridW / cols
        val top = h * 0.13f
        for (i in 1..LevelCatalog.LEVEL_COUNT) {
            val col = (i - 1) % cols
            val row = (i - 1) / cols
            val cx = w * 0.06f + col * cell
            val cy = top + row * cell
            val r = RectF(cx + cell * 0.08f, cy + cell * 0.08f, cx + cell * 0.92f, cy + cell * 0.92f)
            buttonRects["lvl$i"] = r
            val unlocked = progress.isUnlocked(i)
            paint.color = when {
                i == currentLevel -> pipeActive
                unlocked -> pipeColor
                else -> lockedColor
            }
            canvas.drawRoundRect(r, cell * 0.18f, cell * 0.18f, paint)
            text(canvas, "$i", cx + cell / 2, cy + cell * 0.62f, cell * 0.34f,
                color = if (unlocked) Color.WHITE else textColor)
            val stars = progress.starsForLevel(i)
            for (s in 0 until stars) {
                paint.color = starColor
                canvas.drawCircle(cx + cell * (0.32f + 0.18f * s), cy + cell * 0.86f, cell * 0.055f, paint)
            }
        }
        button(canvas, "back", "Назад", w / 2, h * 0.955f, w * 0.4f, h * 0.055f)
    }

    private fun drawPlay(canvas: Canvas, dimmed: Boolean = false) {
        val w = width.toFloat()
        val h = height.toFloat()
        text(canvas, "Уровень $currentLevel", w * 0.5f, h * 0.06f, w * 0.055f)
        text(canvas, "ходы: ${board.moveCount()}  par: $par", w * 0.5f, h * 0.10f, w * 0.04f)
        button(canvas, "back", "←", w * 0.09f, h * 0.06f, w * 0.11f, h * 0.05f)
        button(canvas, "hint", "подсказка ($hintsLeft)", w * 0.79f, h * 0.06f, w * 0.32f, h * 0.05f)

        val size = board.size
        val boardW = min(w * 0.92f, h * 0.62f)
        cellSize = boardW / size
        val left = (w - boardW) / 2
        val top = h * 0.17f
        boardRect = RectF(left, top, left + boardW, top + boardW)

        paint.color = tileFill
        canvas.drawRoundRect(boardRect, cellSize * 0.12f, cellSize * 0.12f, paint)

        for (y in 0 until size) {
            for (x in 0 until size) {
                val idx = y * size + x
                drawTile(canvas, left + x * cellSize, top + y * cellSize, cellSize,
                    board.tileAt(x, y),
                    if (idx in locked) lockedColor else pipeColor,
                    if (idx in locked) 1 else 0)
            }
        }
        if (dimmed) {
            paint.color = Color.argb(150, 247, 241, 232)
            canvas.drawRect(0f, 0f, w, h, paint)
        }
    }

    private fun drawTile(canvas: Canvas, x: Float, y: Float, size: Float, tile: Tile, color: Int, dots: Int) {
        val cx = x + size / 2
        val cy = y + size / 2
        val half = size / 2
        val stroke = size * 0.16f
        paint.color = color
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = stroke
        paint.style = Paint.Style.STROKE
        if (tile.north) canvas.drawLine(cx, cy, cx, y + stroke * 0.3f, paint)
        if (tile.south) canvas.drawLine(cx, cy, cx, y + size - stroke * 0.3f, paint)
        if (tile.west) canvas.drawLine(cx, cy, x + stroke * 0.3f, cy, paint)
        if (tile.east) canvas.drawLine(cx, cy, x + size - stroke * 0.3f, cy, paint)
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx, cy, stroke * 0.75f, paint)
        paint.color = lockedColor
        for (d in 0 until dots) {
            canvas.drawCircle(cx - stroke * 0.5f + d * stroke * 0.5f, cy + size * 0.32f, stroke * 0.18f, paint)
        }
    }

    private fun drawWin(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        text(canvas, "Уровень пройден!", w / 2, h * 0.38f, w * 0.08f)
        for (i in 0 until 3) {
            paint.color = if (i < lastStars) starColor else lockedColor
            canvas.drawCircle(w / 2 + (i - 1) * w * 0.12f, h * 0.46f, w * 0.045f, paint)
        }
        text(canvas, "ходов: ${board.moveCount()} (par $par)", w / 2, h * 0.53f, w * 0.045f)
        button(canvas, "next", "Дальше", w / 2, h * 0.63f, w * 0.5f, h * 0.065f)
        button(canvas, "back", "К уровням", w / 2, h * 0.72f, w * 0.5f, h * 0.065f)
    }
}
