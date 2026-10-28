package pathlink

import org.junit.Test
import org.junit.Assert.*
import com.pathlink.game.Board
import com.pathlink.game.Tile

/**
 * Тесты правил головоломки: вращение, проверка решения, обрывы связей, подсказки.
 * Фикстуры построены так, чтобы открытые стороны всегда были взаимны (кроме теста обрыва).
 */
class SolverTest {

    /** Доска 2x2 с циклом (0,0)-(1,0)-(1,1)-(0,1)-(0,0): решена в нулевом состоянии. */
    private fun solved2x2(): Board {
        val tiles = arrayOf(
            Tile(north = false, east = true, south = true, west = false),  // (0,0)
            Tile(north = false, east = false, south = true, west = true),  // (1,0)
            Tile(north = true, east = false, south = false, west = true),  // (1,1)
            Tile(north = true, east = true, south = false, west = false)   // (0,1)
        )
        return Board(2, tiles, generated = tiles.map { it.copy() }.toTypedArray())
    }

    @Test
    fun rotating_a_tile_breaks_the_solution() {
        val b = solved2x2()
        assertTrue("Исходная доска должна быть решена", b.isSolved())
        b.rotate(0, 0)
        assertFalse("После поворота доска не должна быть решена", b.isSolved())
    }

    @Test
    fun four_rotations_restore_the_solution() {
        val b = solved2x2()
        b.rotate(0, 0); b.rotate(0, 0); b.rotate(0, 0); b.rotate(0, 0)
        assertTrue("Четыре поворота возвращают исходное состояние", b.isSolved())
    }

    @Test
    fun dangling_connection_is_not_a_solution() {
        // (0,0) смотрит на восток, но (1,0) не принимает связь — обрыв
        val tiles = arrayOf(
            Tile(north = false, east = true, south = true, west = false),  // (0,0): E оборван, S взаимна
            Tile(north = false, east = false, south = true, west = false), // (1,0): только S
            Tile(north = true, east = false, south = false, west = false), // (1,1): принимает (1,0)
            Tile(north = true, east = false, south = false, west = false)  // (0,1): принимает (0,0)
        )
        val b = Board(2, tiles, generated = tiles.map { it.copy() }.toTypedArray())
        assertFalse("Обрыв связи не считается решением", b.isSolved())
    }

    @Test
    fun flood_fill_detects_disconnected_groups() {
        // Две раздельные вертикальные пары: все связи взаимны, но компоненты разобщены
        val tiles = arrayOf(
            Tile(north = false, east = false, south = true, west = false),  // (0,0)-(0,1)
            Tile(north = false, east = false, south = true, west = false),  // (1,0)-(1,1)
            Tile(north = true, east = false, south = false, west = false),  // (0,1)
            Tile(north = true, east = false, south = false, west = false)   // (1,1)
        )
        val b = Board(2, tiles, generated = tiles.map { it.copy() }.toTypedArray())
        assertFalse("Раздельные группы не решают доску", b.isSolved())
    }

    @Test
    fun hint_orients_one_tile_to_solution() {
        val b = BoardGenerator.generate(5, 3L)
        val before = b.signature()
        val idx = b.firstUnsolvedTile()
        assertTrue("На нерешённой доске есть неправильная клетка", idx >= 0)
        b.applyHint(idx)
        assertTrue("После подсказки клетка должна стоять в решении", b.tileMatchesSolution(idx))
        assertNotEquals("Подсказка должна менять доску", before, b.signature())
    }

    @Test
    fun moves_counter_increments_per_rotation() {
        val b = solved2x2()
        b.rotate(0, 0)
        assertEquals("Счётчик ходов должен увеличиться", 1, b.moveCount())
        b.rotate(0, 0)
        assertEquals("Поворот той же клетки — ещё ход", 2, b.moveCount())
    }
}
