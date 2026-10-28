package pathlink

import org.junit.Test
import org.junit.Assert.*
import com.pathlink.game.Board
import com.pathlink.game.BoardGenerator

/**
 * Тесты генератора головоломки (ось C: процедурный генератор графов с гарантированным решением).
 * Генератор строит остовное дерево сетки, из него — формы плиток, затем рандомизирует повороты.
 * Решение гарантировано существует.
 */
class PathGeneratorTest {

    @Test
    fun `solution state is always connected`() {
        for (size in 4..8) {
            for (seed in 1L..20L) {
                val board = BoardGenerator.generate(size, seed)
                assertTrue("size=$size seed=$seed: эталонное состояние должно быть решённым",
                    board.isSolvedInGeneratedOrientation())
            }
        }
    }

    @Test
    fun `same seed produces identical board`() {
        val a = BoardGenerator.generate(5, 42L)
        val b = BoardGenerator.generate(5, 42L)
        assertEquals("Генератор должен быть детерминированным", a.signature(), b.signature())
    }

    @Test
    fun `different seeds produce different boards`() {
        val a = BoardGenerator.generate(6, 1L)
        val b = BoardGenerator.generate(6, 2L)
        assertNotEquals("Разные сиды должны давать разные доски", a.signature(), b.signature())
    }

    @Test
    fun `scrambled board is not solved`() {
        var scrambledCount = 0
        for (seed in 1L..30L) {
            val board = BoardGenerator.generate(5, seed)
            if (!board.isSolved()) scrambledCount++
        }
        assertTrue("Большинство сгенерированных досок должны быть не решены", scrambledCount >= 25)
    }

    @Test
    fun `par moves is positive and bounded`() {
        for (seed in 1L..30L) {
            val board = BoardGenerator.generate(6, seed)
            val par = board.parMoves()
            assertTrue("par должен быть > 0 (seed=$seed)", par > 0)
            assertTrue("par не должен превышать 3 * клетки (seed=$seed)", par <= 3 * 36)
        }
    }

    @Test
    fun `every cell has at least one connection`() {
        val board = BoardGenerator.generate(7, 7L)
        for (y in 0 until board.size) {
            for (x in 0 until board.size) {
                val t = board.tileAt(x, y)
                assertTrue("У клетки ($x,$y) должна быть хотя бы одна связь",
                    t.north || t.east || t.south || t.west)
            }
        }
    }
}
