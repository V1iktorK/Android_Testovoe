package com.wasteland.game

/** Крафт-дерево выживания: рецепты из сырья и созданных предметов (без циклов). */
class CraftingBook {

    private val recipes: Map<String, Map<String, Int>> = mapOf(
        "campfire" to mapOf("wood" to 3),
        "bandage" to mapOf("cloth" to 2),
        "clean_water" to mapOf("water" to 1, "cloth" to 1),
        "stew" to mapOf("canned_food" to 1, "water" to 1, "campfire" to 1),
        "warmcoat" to mapOf("cloth" to 4, "tools" to 1),
        "generator" to mapOf("electronics" to 2, "fuel" to 1, "tools" to 1),
        "weapon" to mapOf("weapon_parts" to 2, "tools" to 1)
    )

    /** Ингредиенты рецепта. */
    fun ingredientsFor(item: String): Map<String, Int> = recipes[item] ?: emptyMap()

    /** Есть ли рецепт. */
    fun hasRecipe(item: String): Boolean = recipes.containsKey(item)

    /** Проверка крафт-дерева на циклы (готовые предметы могут быть ингредиентами). */
    fun hasCycles(): Boolean {
        val visiting = HashSet<String>()
        val visited = HashSet<String>()

        fun dfs(node: String): Boolean {
            if (node in visiting) return true
            if (node in visited) return false
            visiting.add(node)
            for (ing in (recipes[node] ?: emptyMap()).keys) {
                if (recipes.containsKey(ing) && dfs(ing)) return true
            }
            visiting.remove(node)
            visited.add(node)
            return false
        }

        for (item in recipes.keys) {
            if (dfs(item)) return true
        }
        return false
    }

    /** Крафт: списывает ингредиенты и добавляет предмет. false — не хватило ресурсов. */
    fun craft(inventory: MutableMap<String, Int>, item: String): Boolean {
        val ingredients = recipes[item] ?: return false
        for ((ing, need) in ingredients) {
            if ((inventory[ing] ?: 0) < need) return false
        }
        for ((ing, need) in ingredients) {
            inventory[ing] = (inventory[ing] ?: 0) - need
        }
        inventory[item] = (inventory[item] ?: 0) + 1
        return true
    }

    /** Все известные рецепты. */
    fun all(): Set<String> = recipes.keys
}
