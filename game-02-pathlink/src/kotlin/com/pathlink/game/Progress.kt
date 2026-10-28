package com.pathlink.game

/**
 * Каталог уровней: 60 уровней, растущая сложность (ось C: детерминированные сиды).
 */
data class LevelParams(val boardSize: Int, val difficulty: Int, val seed: Long)

object LevelCatalog {
    const val LEVEL_COUNT = 60

    fun paramsFor(level: Int): LevelParams {
        require(level in 1..LEVEL_COUNT) { "Уровень вне диапазона: $level" }
        val boardSize = when {
            level <= 15 -> 4
            level <= 30 -> 5
            level <= 45 -> 6
            else -> 7
        }
        // Сложность 1..5: каждые 12 уровней +1
        val difficulty = 1 + (level - 1) / 12
        val seed = 7919L * level + 104729L
        return LevelParams(boardSize, difficulty.coerceAtMost(5), seed)
    }
}

/**
 * Прогресс игрока: звёзды за уровни, правила открытия, сериализация.
 */
class Progress {
    private val stars = mutableMapOf<Int, Int>()
    private val bestMoves = mutableMapOf<Int, Int>()

    fun complete(level: Int, stars: Int, moves: Int) {
        val s = stars.coerceIn(1, 3)
        this.stars[level] = maxOf(this.stars[level] ?: 0, s)
        val best = bestMoves[level]
        if (best == null || moves < best) bestMoves[level] = moves
    }

    fun starsForLevel(level: Int): Int = stars[level] ?: 0

    fun bestMovesForLevel(level: Int): Int = bestMoves[level] ?: 0

    fun totalStars(): Int = stars.values.sum()

    fun isUnlocked(level: Int): Boolean =
        level == 1 || starsForLevel(level - 1) > 0

    fun serialize(): String =
        (1..LevelCatalog.LEVEL_COUNT)
            .filter { stars.containsKey(it) }
            .joinToString(";") { "$it:${stars[it]}:${bestMoves[it] ?: 0}" }

    companion object {
        /** Рейтинг: 3 звезды — ровно par, 2 — до 1.5*par, иначе 1. */
        fun starsFor(moves: Int, par: Int): Int = when {
            moves <= par -> 3
            moves <= par * 3 / 2 -> 2
            else -> 1
        }

        fun deserialize(text: String): Progress {
            val p = Progress()
            if (text.isBlank()) return p
            for (entry in text.split(";")) {
                val parts = entry.split(":")
                if (parts.size >= 2) {
                    val level = parts[0].toIntOrNull() ?: continue
                    val s = parts[1].toIntOrNull() ?: continue
                    val m = parts.getOrNull(2)?.toIntOrNull() ?: 0
                    p.complete(level, s, m)
                }
            }
            return p
        }
    }
}
