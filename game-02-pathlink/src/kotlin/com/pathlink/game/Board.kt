package com.pathlink.game

/**
 * Плитка головоломки: открытые стороны света (N/E/S/W).
 * Поворот по часовой стрелке на 90°.
 */
data class Tile(val north: Boolean, val east: Boolean, val south: Boolean, val west: Boolean) {

    fun rotatedCW(): Tile = Tile(west, north, east, south)

    /** Поворот на k*90° по часовой стрелке. */
    fun rotated(times: Int): Tile {
        var t = this
        repeat(((times % 4) + 4) % 4) { t = t.rotatedCW() }
        return t
    }

    /** Число открытых сторон. */
    fun degree(): Int = (if (north) 1 else 0) + (if (east) 1 else 0) + (if (south) 1 else 0) + (if (west) 1 else 0)
}

/**
 * Игровая доска NxN. Хранит текущие повороты плиток и эталонное (решённое) состояние.
 */
class Board(
    val size: Int,
    private val tiles: Array<Tile>,
    private val generated: Array<Tile>
) {
    private var moves: Int = 0

    fun tileAt(x: Int, y: Int): Tile = tiles[y * size + x]

    fun moveCount(): Int = moves

    fun rotate(x: Int, y: Int) {
        val i = y * size + x
        tiles[i] = tiles[i].rotatedCW()
        moves++
    }

    /** Решена ли доска: связность всех клеток + отсутствие обрывов. */
    fun isSolved(): Boolean {
        val visited = BooleanArray(size * size)
        val stack = ArrayDeque<Int>()
        stack.addLast(0)
        visited[0] = true
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            val x = i % size
            val y = i / size
            val t = tiles[i]
            if (t.north) {
                if (y == 0 || !tiles[i - size].south) return false // обрыв
                if (!visited[i - size]) { visited[i - size] = true; stack.addLast(i - size) }
            }
            if (t.south) {
                if (y == size - 1 || !tiles[i + size].north) return false
                if (!visited[i + size]) { visited[i + size] = true; stack.addLast(i + size) }
            }
            if (t.west) {
                if (x == 0 || !tiles[i - 1].east) return false
                if (!visited[i - 1]) { visited[i - 1] = true; stack.addLast(i - 1) }
            }
            if (t.east) {
                if (x == size - 1 || !tiles[i + 1].west) return false
                if (!visited[i + 1]) { visited[i + 1] = true; stack.addLast(i + 1) }
            }
        }
        return visited.all { it }
    }

    /** Решено ли эталонное состояние (проверка генератора). */
    fun isSolvedInGeneratedOrientation(): Boolean {
        val snapshot = tiles.copyOf()
        for (i in tiles.indices) tiles[i] = generated[i]
        val ok = isSolved()
        for (i in tiles.indices) tiles[i] = snapshot[i]
        return ok
    }

    /** Сериализация состояния для сравнения и детерминизма. */
    fun signature(): String =
        tiles.joinToString("|") { "${it.north}${it.east}${it.south}${it.west}" } +
            "#" + generated.joinToString("|") { "${it.north}${it.east}${it.south}${it.west}" }

    /** Минимальное число поворотов по часовой стрелке до решения (par). */
    fun parMoves(): Int {
        var total = 0
        for (i in tiles.indices) {
            var t = tiles[i]
            var k = 0
            while (t != generated[i] && k < 4) {
                t = t.rotatedCW()
                k++
            }
            total += k
        }
        return total
    }

    /** Индекс первой плитки, отличающейся от решения, или -1. */
    fun firstUnsolvedTile(): Int {
        for (i in tiles.indices) if (tiles[i] != generated[i]) return i
        return -1
    }

    fun tileMatchesSolution(index: Int): Boolean = tiles[index] == generated[index]

    /** Подсказка: возвращает плитку в решение. */
    fun applyHint(index: Int) {
        tiles[index] = generated[index]
    }
}

/**
 * Процедурный генератор головоломок (ось C уникальности):
 * случайное остовное дерево сетки -> формы плиток -> рандомизация поворотов.
 * Решение гарантированно существует (это порождающее состояние).
 */
object BoardGenerator {

    fun generate(size: Int, seed: Long): Board {
        val rnd = java.util.Random(seed)
        val n = size * size
        // --- Остовное дерево: рандомизированный DFS ---
        val parent = IntArray(n) { -1 }
        val visited = BooleanArray(n)
        val stack = ArrayDeque<Int>()
        stack.addLast(0)
        visited[0] = true
        while (stack.isNotEmpty()) {
            val i = stack.removeLast()
            val x = i % size
            val y = i / size
            val dirs = mutableListOf<Int>()
            if (y > 0 && !visited[i - size]) dirs.add(i - size)
            if (y < size - 1 && !visited[i + size]) dirs.add(i + size)
            if (x > 0 && !visited[i - 1]) dirs.add(i - 1)
            if (x < size - 1 && !visited[i + 1]) dirs.add(i + 1)
            if (dirs.isEmpty()) continue
            // Перемешиваем соседей
            for (j in dirs.indices - 1 downTo 1) {
                val k = rnd.nextInt(j + 1)
                val tmp = dirs[j]; dirs[j] = dirs[k]; dirs[k] = tmp
            }
            for (nb in dirs) {
                if (!visited[nb]) {
                    visited[nb] = true
                    parent[nb] = i
                    stack.addLast(nb)
                }
            }
        }

        // --- Формы плиток из дерева ---
        val solved = Array(n) { Tile(false, false, false, false) }
        for (i in 1 until n) {
            val p = parent[i]
            val dx = (i % size) - (p % size)
            val dy = (i / size) - (p / size)
            var a = solved[i]
            var b = solved[p]
            when {
                dx == 1 -> { a = a.copy(west = true); b = b.copy(east = true) }
                dx == -1 -> { a = a.copy(east = true); b = b.copy(west = true) }
                dy == 1 -> { a = a.copy(north = true); b = b.copy(south = true) }
                dy == -1 -> { a = a.copy(south = true); b = b.copy(north = true) }
            }
            solved[i] = a
            solved[p] = b
        }

        // --- Рандомизация поворотов ---
        val scrambled = Array(n) { solved[it].rotated(rnd.nextInt(4)) }
        return Board(size, scrambled, solved)
    }
}
