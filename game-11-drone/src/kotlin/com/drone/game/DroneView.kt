package com.drone.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * «Дрон-Патруль» — шутер сверху: дрон летит к пальцу, стреляет по врагам автоматически.
 * Киберпанк: тёмная сетка, неоновые контуры, автозавод на фоне.
 */
class DroneView(context: Context) : View(context) {

    private enum class State { Menu, Play, Upgrade, GameOver }

    private var state = State.Menu
    private val drone = DroneState()
    private var wave = 1
    private var score = 0
    private var bestWave = 0
    private var droneX = 540f
    private var droneY = 1500f
    private var targetX = 540f
    private var targetY = 1500f
    private var health = 100f
    private var fireTimer = 0f
    private val bullets = ArrayList<Bullet>()
    private val enemies = ArrayList<Enemy>()
    private var waveTimer = 0f
    private var bannerTime = 0f
    private var hitFlash = 0f
    private val sound = DroneSound()
    private var lastTime = SystemClock.elapsedRealtime()
    private val buttons = ArrayList<RectF>()
    private val buttonIds = ArrayList<Int>()

    private class Bullet(var x: Float, var y: Float, val vx: Float, val vy: Float, val damage: Float)
    private class Enemy(var x: Float, var y: Float, var hp: Float, val speed: Float, val damage: Float,
                        val type: WaveDirector.Type, val reward: Int)

    private val bg = Color.rgb(10, 12, 22)
    private val grid = Color.rgb(24, 30, 48)
    private val neonCyan = Color.rgb(34, 225, 255)
    private val neonPink = Color.rgb(255, 42, 140)
    private val textLight = Color.rgb(220, 232, 255)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        loadBest()
    }

    private fun loadBest() {
        bestWave = context.getSharedPreferences("drone", Context.MODE_PRIVATE).getInt("best", 0)
    }

    private fun saveBest() {
        if (wave > bestWave) {
            bestWave = wave
            context.getSharedPreferences("drone", Context.MODE_PRIVATE).edit().putInt("best", bestWave).apply()
        }
    }

    // ================== Игровой цикл ==================

    private fun startGame() {
        wave = 1
        score = 0
        health = 100f
        drone.shield = 50f
        bullets.clear()
        spawnWave()
        state = State.Play
    }

    private fun spawnWave() {
        enemies.clear()
        val performance = (health / 100f + drone.shield / 100f) / 2f * 2.0
        val specs = WaveDirector.generate(20261028, wave, performance)
        for (s in specs) {
            val angle = Math.random() * Math.PI * 2
            enemies.add(Enemy(
                (540f + cos(angle) * 900f).toFloat(),
                (1000f + sin(angle) * 900f).toFloat(),
                s.hp, s.speed, s.damage, s.type, s.reward
            ))
        }
        bannerTime = 2.2f
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
                State.Upgrade -> drawUpgrade(canvas)
                State.GameOver -> drawGameOver(canvas)
                else -> {}
            }
        }
        if (hitFlash > 0f) {
            paint.color = Color.argb((hitFlash * 110).toInt(), 255, 40, 60)
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            hitFlash -= dt * 2.5f
        }
        postInvalidateDelayed(28)
    }

    private fun step(dt: Float) {
        // Движение дрона к пальцу
        val dx = targetX - droneX
        val dy = targetY - droneY
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        if (dist > 8f) {
            droneX += dx / dist * 620f * dt
            droneY += dy / dist * 620f * dt
        }

        // Автострельба по ближайшему врагу
        fireTimer -= dt
        if (fireTimer <= 0f && enemies.isNotEmpty()) {
            var nearest = enemies[0]
            var best = Float.MAX_VALUE
            for (e in enemies) {
                val d2 = (e.x - droneX) * (e.x - droneX) + (e.y - droneY) * (e.y - droneY)
                if (d2 < best) {
                    best = d2
                    nearest = e
                }
            }
            val ang = atan2(nearest.y - droneY, nearest.x - droneX)
            if (drone.canFireAt(waveTimer)) {
                drone.fire(waveTimer)
                bullets.add(Bullet(droneX, droneY, cos(ang) * 1150f, sin(ang) * 1150f, drone.bulletDamage()))
                fireTimer = 1f / drone.fireRate
            }
        }
        waveTimer += dt

        // Пули
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.x += b.vx * dt
            b.y += b.vy * dt
            if (b.x < -100 || b.x > 1180 || b.y < -100 || b.y > 2100) {
                it.remove()
                continue
            }
            for (e in enemies) {
                if (CombatMath.hits(b.x, b.y, e.x, e.y, 70f)) {
                    e.hp -= CombatMath.damageAfterArmor(b.damage, if (e.type == WaveDirector.Type.Tank) 6f else 0f)
                    it.remove()
                    break
                }
            }
        }

        // Враги: движение к дрону, урон при контакте
        val eIt = enemies.iterator()
        while (eIt.hasNext()) {
            val e = eIt.next()
            val adx = droneX - e.x
            val ady = droneY - e.y
            val adist = kotlin.math.sqrt(adx * adx + ady * ady)
            if (adist > 70f) {
                e.x += adx / adist * e.speed * 130f * dt
                e.y += ady / adist * e.speed * 130f * dt
            } else {
                val taken = drone.takeDamage(e.damage * dt * 2f)
                if (taken > 0f) {
                    health -= taken
                    hitFlash = 0.6f
                }
                if (health <= 0f) {
                    saveBest()
                    sound.explode()
                    state = State.GameOver
                }
            }
            if (e.hp <= 0f) {
                score += e.reward
                eIt.remove()
            }
        }

        // Волна пройдена
        if (enemies.isEmpty() && state == State.Play) {
            score += CombatMath.scoreFor(wave, 5)
            wave++
            saveBest()
            state = State.Upgrade
        }
    }

    private fun drawBackground(canvas: Canvas) {
        canvas.drawColor(bg)
        paint.color = grid
        paint.strokeWidth = 2f
        for (x in 0..1080 step 120) canvas.drawLine(x.toFloat(), 0f, x.toFloat(), height.toFloat(), paint)
        for (y in 0..2000 step 120) canvas.drawLine(0f, y.toFloat(), width.toFloat(), y.toFloat(), paint)
    }

    private fun drawWorld(canvas: Canvas) {
        // Пули
        paint.color = neonCyan
        for (b in bullets) canvas.drawCircle(b.x, b.y, 12f, paint)
        // Враги
        for (e in enemies) {
            val color = when (e.type) {
                WaveDirector.Type.Scout -> Color.rgb(255, 84, 140)
                WaveDirector.Type.Sprinter -> Color.rgb(255, 170, 60)
                WaveDirector.Type.Tank -> Color.rgb(170, 80, 230)
                WaveDirector.Type.Turret -> Color.rgb(120, 200, 255)
                WaveDirector.Type.Boss -> Color.rgb(255, 40, 40)
            }
            paint.color = color
            val r = if (e.type == WaveDirector.Type.Boss) 130f else 66f
            canvas.drawCircle(e.x, e.y, r, paint)
            paint.color = Color.argb(90, 255, 255, 255)
            canvas.drawCircle(e.x, e.y, r * 0.55f, paint)
        }
        // Дрон (треугольник + винты)
        paint.color = neonCyan
        val path = Path()
        val ang = atan2(targetY - droneY, targetX - droneX)
        path.moveTo(droneX + cos(ang) * 70f, droneY + sin(ang) * 70f)
        path.lineTo(droneX + cos(ang + 2.5f) * 60f, droneY + sin(ang + 2.5f) * 60f)
        path.lineTo(droneX + cos(ang - 2.5f) * 60f, droneY + sin(ang - 2.5f) * 60f)
        path.close()
        canvas.drawPath(path, paint)
        paint.color = neonPink
        canvas.drawCircle(droneX, droneY, 22f, paint)
    }

    private fun drawHud(canvas: Canvas) {
        // уголки телеметрии дрона
        paint.color = neonCyan
        paint.strokeWidth = 8f
        val m = 30f
        val len = 90f
        canvas.drawLine(m, m, m + len, m, paint)
        canvas.drawLine(m, m, m, m + len, paint)
        canvas.drawLine(width - m, m, width - m - len, m, paint)
        canvas.drawLine(width - m, m, width - m, m + len, paint)
        canvas.drawLine(m, height - m, m + len, height - m, paint)
        canvas.drawLine(m, height - m, m, height - m - len, paint)
        canvas.drawLine(width - m, height - m, width - m - len, height - m, paint)
        canvas.drawLine(width - m, height - m, width - m, height - m - len, paint)
        val w = width.toFloat()
        paint.textAlign = Paint.Align.LEFT
        paint.color = textLight
        paint.textSize = w * 0.048f
        canvas.drawText("Волна $wave", w * 0.04f, w * 0.1f, paint)
        paint.textSize = w * 0.038f
        paint.color = Color.rgb(120, 255, 170)
        canvas.drawText("корпус: ${health.toInt()}", w * 0.04f, w * 0.15f, paint)
        paint.color = Color.rgb(120, 200, 255)
        canvas.drawText("щит: ${drone.shield.toInt()}", w * 0.04f, w * 0.195f, paint)
        paint.textAlign = Paint.Align.RIGHT
        paint.color = textLight
        canvas.drawText("очки: $score", w * 0.96f, w * 0.1f, paint)
        if (bannerTime > 0f) {
            bannerTime -= 0.028f
            paint.textAlign = Paint.Align.CENTER
            paint.color = Color.argb((min(bannerTime, 1f) * 255).toInt(), 255, 60, 140)
            paint.textSize = w * 0.09f
            canvas.drawText("ВОЛНА $wave", w / 2f, w * 0.55f, paint)
        }
    }

    private fun drawMenu(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = neonCyan
        paint.textSize = w * 0.11f
        canvas.drawText("ДРОН-ПАТРУЛЬ", w / 2f, h * 0.3f, paint)
        paint.color = textLight
        paint.textSize = w * 0.042f
        canvas.drawText("киберпанк-шутер: дрон следует за пальцем", w / 2f, h * 0.36f, paint)
        canvas.drawText("и сам расстреливает врагов автозавода", w / 2f, h * 0.41f, paint)
        button(canvas, 0, "В бой", w / 2f, h * 0.55f, w * 0.55f, h * 0.075f)
        paint.textSize = w * 0.038f
        paint.color = Color.rgb(150, 170, 200)
        canvas.drawText("лучшая волна: $bestWave", w / 2f, h * 0.66f, paint)
    }

    private fun drawUpgrade(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = neonCyan
        paint.textSize = w * 0.09f
        canvas.drawText("Волна $wave зачищена!", w / 2f, h * 0.28f, paint)
        paint.color = textLight
        paint.textSize = w * 0.042f
        canvas.drawText("выбери улучшение", w / 2f, h * 0.34f, paint)
        button(canvas, 1, "Щит +25", w / 2f, h * 0.46f, w * 0.6f, h * 0.07f)
        button(canvas, 2, "Урон +35%", w / 2f, h * 0.56f, w * 0.6f, h * 0.07f)
        button(canvas, 3, "Скорострельность +20%", w / 2f, h * 0.66f, w * 0.6f, h * 0.07f)
    }

    private fun drawGameOver(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        paint.textAlign = Paint.Align.CENTER
        paint.color = neonPink
        paint.textSize = w * 0.1f
        canvas.drawText("ДРОН СБИТ", w / 2f, h * 0.32f, paint)
        paint.color = textLight
        paint.textSize = w * 0.05f
        canvas.drawText("волна: $wave   очки: $score", w / 2f, h * 0.39f, paint)
        button(canvas, 0, "Ещё раз", w / 2f, h * 0.52f, w * 0.55f, h * 0.075f)
        button(canvas, 4, "В меню", w / 2f, h * 0.63f, w * 0.55f, h * 0.075f)
    }

    private fun button(canvas: Canvas, id: Int, label: String, cx: Float, cy: Float, bw: Float, bh: Float) {
        val r = RectF(cx - bw / 2, cy - bh / 2, cx + bw / 2, cy + bh / 2)
        buttons.add(r)
        buttonIds.add(id)
        paint.color = if (id == 0) neonPink else Color.rgb(30, 42, 70)
        canvas.drawRoundRect(r, bh / 2, bh / 2, paint)
        paint.color = Color.WHITE
        paint.textSize = bh * 0.42f
        canvas.drawText(label, cx, cy + bh * 0.15f, paint)
    }

    // ================== Касания ==================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                targetX = event.x
                targetY = event.y
                if (state == State.Play) return true
                if (event.actionMasked == MotionEvent.ACTION_DOWN) handleTap(event.x, event.y)
            }
        }
        return true
    }

    private fun handleTap(x: Float, y: Float) {
        for (i in buttons.indices) {
            if (!buttons[i].contains(x, y)) continue
            when (buttonIds[i]) {
                0 -> startGame()
                1 -> { drone.shield += 25f; nextWave() }
                2 -> { drone.upgradeDamage(); nextWave() }
                3 -> { drone.fireRate *= 1.2f; nextWave() }
                4 -> state = State.Menu
            }
            return
        }
    }

    private fun nextWave() {
        spawnWave()
        state = State.Play
    }
}
