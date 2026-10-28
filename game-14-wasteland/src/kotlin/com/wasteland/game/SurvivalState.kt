package com.wasteland.game

import kotlin.math.max
import kotlin.math.min

/**
 * Состояние выживания: голод, тепло, здоровье; цикл дня и ночи.
 * Голод растёт, тепло падает (ночью сильнее), крайние состояния бьют по здоровью.
 */
class SurvivalState {
    var health: Double = 100.0
        private set
    var hunger: Double = 0.0
        private set
    var warmth: Double = 100.0
        private set

    fun setHunger(value: Double) {
        hunger = value.coerceIn(0.0, 100.0)
    }

    /** Еда восстанавливает голод (уменьшает). */
    fun eat(amount: Double) {
        hunger = max(0.0, hunger - amount)
    }

    /** Тепло от костра. */
    fun warmUp(amount: Double) {
        warmth = min(100.0, warmth + amount)
    }

    /** Тик времени: isDay=true — день, false — ночь. */
    fun tick(isDay: Boolean) {
        hunger = min(100.0, hunger + 5.0)
        warmth = max(0.0, warmth - if (isDay) 3.0 else 8.0)
        if (hunger >= 100.0) health = max(0.0, health - 5.0)
        if (warmth <= 0.0) health = max(0.0, health - 5.0)
    }

    fun isAlive(): Boolean = health > 0.0

    companion object {
        /** Очки выживания растут с числом прожитых дней. */
        @JvmStatic
        fun scoreFor(days: Int): Int = days * 150
    }
}
