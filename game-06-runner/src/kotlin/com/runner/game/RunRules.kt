package com.runner.game

/** Позиция игрока по полосам и состояния прыжка/кувырка. */
class LaneState {
    companion object {
        const val LANE_COUNT = 3
    }

    var current: Int = 1
        private set
    var isJumping: Boolean = false
        private set
    var isRolling: Boolean = false
        private set

    fun swipeLeft() {
        if (current > 0) current--
    }

    fun swipeRight() {
        if (current < LANE_COUNT - 1) current++
    }

    fun swipeUp() {
        isJumping = true
        isRolling = false
    }

    fun swipeDown() {
        isRolling = true
        isJumping = false
    }

    /** Завершить прыжок/кувырок (по таймеру в игровом цикле). */
    fun settle() {
        isJumping = false
        isRolling = false
    }
}

/** Правила раннера: столкновения, скоринг, скорость, открытие героев. */
object RunRules {

    const val MAX_SPEED = 18f

    /**
     * Столкновение с препятствием (препятствие, прыжок, кувырок):
     * x стена бьёт всегда; h высокое — безопасно в прыжке; l низкое — безопасно в кувырке.
     */
    @JvmStatic
    fun collides(obstacle: Char, jumping: Boolean, rolling: Boolean): Boolean = when (obstacle) {
        'x' -> true
        'h' -> !jumping
        'l' -> !rolling
        else -> false
    }

    /** Очки: дистанция + монеты (10 очков за монету). */
    @JvmStatic
    fun scoreFor(distance: Int, coins: Int): Int = distance + coins * 10

    /** Скорость растёт с дистанцией. */
    @JvmStatic
    fun speedAt(distance: Int): Float = minOf(8f + distance * 0.002f, MAX_SPEED)

    /** Герои открываются по лучшей дистанции. */
    @JvmStatic
    fun isUnlocked(character: Int, bestDistance: Int): Boolean = when (character) {
        1 -> true
        2 -> bestDistance >= 2000
        3 -> bestDistance >= 5000
        else -> false
    }
}
