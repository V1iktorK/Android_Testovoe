package com.wasteland.game

import java.util.Random

/** Добыча: предмет и его ценность. */
class LootDrop(@JvmField val item: String, @JvmField val value: Int)

/**
 * Лут-таблицы локаций (ось C уникальности): детерминированная добыча по сиду, локации и дню.
 * Чем реже локация и позже день — тем богаче добыча.
 */
object LootTable {

    /** Все известные предметы мира. */
    @JvmField
    val KNOWN_ITEMS = setOf(
        "wood", "scrap", "canned_food", "water", "medicine", "cloth",
        "fuel", "electronics", "tools", "weapon_parts"
    )

    private val SPOT_TABLES = mapOf(
        "street" to listOf("wood", "scrap", "cloth", "water"),
        "hospital" to listOf("medicine", "water", "canned_food", "cloth"),
        "factory" to listOf("scrap", "tools", "electronics", "fuel"),
        "military" to listOf("weapon_parts", "fuel", "electronics", "canned_food")
    )

    private val VALUES = mapOf(
        "wood" to 2, "scrap" to 3, "canned_food" to 6, "water" to 5, "medicine" to 10,
        "cloth" to 3, "fuel" to 8, "electronics" to 12, "tools" to 9, "weapon_parts" to 15
    )

    /** Детерминированная добыча: количество и состав зависят от локации и дня. */
    @JvmStatic
    fun generate(seed: Long, spot: String, day: Int): List<LootDrop> {
        val rng = Random(seed * 31 + day * 7919L)
        val table = SPOT_TABLES[spot] ?: SPOT_TABLES["street"]!!
        val rarity = when (spot) {
            "military" -> 2.0
            "factory", "hospital" -> 1.4
            else -> 1.0
        }
        val count = 2 + (rarity + day * 0.15).toInt()
        val drops = ArrayList<LootDrop>(count)
        repeat(count) {
            val item = table[rng.nextInt(table.size)]
            val value = ((VALUES[item] ?: 3) * rarity * (1.0 + day * 0.05)).toInt()
            drops.add(LootDrop(item, value))
        }
        return drops
    }

    @JvmStatic
    fun valueOf(item: String): Int = VALUES[item] ?: 0
}
