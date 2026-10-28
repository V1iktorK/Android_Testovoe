package com.drone.game

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Спецификация врага. Поля открыты для JVM-тестов (@JvmField). */
class EnemySpec(
    @JvmField val type: WaveDirector.Type,
    @JvmField val hp: Float,
    @JvmField val speed: Float,
    @JvmField val damage: Float,
    @JvmField val reward: Int
)

/**
 * «Режиссёр волн» (ось C уникальности: адаптивная генерация волн по обратной связи от игрока).
 * Метрика performance (0.5..1.5) масштабирует крепость и состав волны.
 */
object WaveDirector {

    enum class Type { Scout, Tank, Sprinter, Turret, Boss }

    /** Ограничивает метрику производительности игрока диапазоном 0.5..1.5. */
    @JvmStatic
    fun clampPerformance(performance: Double): Double = min(1.5, max(0.5, performance))

    /**
     * Волна: количество и параметры врагов зависят от номера волны и performance.
     * Каждая 5-я волна содержит босса.
     */
    @JvmStatic
    fun generate(seed: Long, wave: Int, performance: Double): List<EnemySpec> {
        val rng = Random(seed + wave * 7919L)
        val p = clampPerformance(performance)
        val count = 3 + wave + (p * 2).toInt()
        val enemies = ArrayList<EnemySpec>(count + 1)
        for (i in 0 until count) {
            val roll = rng.nextDouble()
            val type = when {
                wave % 5 == 0 && i == 0 -> Type.Boss
                roll < 0.45 -> Type.Scout
                roll < 0.7 -> Type.Sprinter
                roll < 0.9 -> Type.Tank
                else -> Type.Turret
            }
            enemies.add(specFor(type, wave, p))
        }
        return enemies
    }

    private fun specFor(type: Type, wave: Int, p: Double): EnemySpec {
        val growth = 1.0 + wave * 0.12
        return when (type) {
            Type.Scout -> EnemySpec(type, (20 * growth * p).toFloat(), 3.2f, 8f, 10)
            Type.Sprinter -> EnemySpec(type, (14 * growth * p).toFloat(), 5.1f, 6f, 15)
            Type.Tank -> EnemySpec(type, (55 * growth * p).toFloat(), 1.8f, 18f, 25)
            Type.Turret -> EnemySpec(type, (35 * growth * p).toFloat(), 0.0f, 12f, 20)
            Type.Boss -> EnemySpec(type, (220 * growth * p).toFloat(), 1.2f, 35f, 200)
        }
    }
}

/** Боевая математика дрона: урон, попадания, апгрейды, скоринг. */
object CombatMath {

    /** Урон после брони. */
    @JvmStatic
    fun damageAfterArmor(damage: Float, armor: Float): Float = max(0f, damage - armor)

    /** Попадает ли пуля (круговое столкновение). */
    @JvmStatic
    fun hits(px: Float, py: Float, ex: Float, ey: Float, radius: Float): Boolean {
        val dx = px - ex
        val dy = py - ey
        return dx * dx + dy * dy <= radius * radius
    }

    /** Стоимость апгрейда уровня level (квадратичный рост). */
    @JvmStatic
    fun upgradeCost(level: Int): Float = 100f * (level + 1) * (level + 1)

    /** Очки за очищенную волну. */
    @JvmStatic
    fun scoreFor(wave: Int, killed: Int): Int = wave * 120 + killed * 25
}

/** Состояние дрона игрока: щит, темп огня, апгрейды. */
class DroneState {
    @JvmField var shield: Float = 50f
    @JvmField var fireRate: Float = 3f
    @JvmField var armor: Float = 0f
    private var damageLevel = 0
    private var lastFire = -100f

    /** Можно ли стрелять в момент t (ограничение темпа огня). */
    fun canFireAt(t: Float): Boolean = t - lastFire >= 1f / fireRate

    fun fire(t: Float) {
        lastFire = t
    }

    /** Урон с учётом апгрейдов. */
    fun bulletDamage(): Float = 12f * (1f + damageLevel * 0.35f)

    fun upgradeDamage() {
        damageLevel++
    }

    /** Поглощает урон щитом; возвращает остаток по корпусу. */
    fun takeDamage(amount: Float): Float {
        val absorbed = min(shield, amount)
        shield -= absorbed
        return amount - absorbed
    }
}
