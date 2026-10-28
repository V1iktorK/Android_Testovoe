package com.runner.game

import kotlin.random.Random

/**
 * Генератор трассы раннера (ось C уникальности: чанковая генерация — грамматика чанков).
 * Алфавит: x стена, h высокое (прыжок), l низкий барьер (кувырок), b монеты, u монеты вверху, . свободно.
 */
object TrackGenerator {

    /** Грамматика: безопасные чанки без трёх стен подряд. */
    private val CHUNKS = arrayOf(
        ".", "..", "...", "....",
        ".b.", ".b", "b..", ".bb.",
        "h..", ".h.", "..h", "h.b.",
        "l..", ".l.", "..l", "l.b.",
        "h.l.", "l.h.", "b.h.", "b.l.",
        ".u.", "u..", "..u",
        "x..", ".x.", "..x", "x.b.", "b.x."
    )

    /** Детерминированная трасса длиной ровно length символов. */
    @JvmStatic
    fun generate(seed: Long, length: Int): String {
        if (length <= 0) return ""
        val rng = Random(seed)
        val bodyLength = maxOf(0, length - 6)
        val sb = StringBuilder(length)
        while (sb.length < bodyLength) {
            sb.append(CHUNKS[rng.nextInt(CHUNKS.size)])
        }
        val result = StringBuilder(sb.substring(0, bodyLength))
        while (result.length < length) result.append('.')
        return result.toString()
    }
}
