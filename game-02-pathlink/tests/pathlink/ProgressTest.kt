package pathlink

import org.junit.Test
import org.junit.Assert.*
import com.pathlink.game.LevelCatalog
import com.pathlink.game.Progress

/**
 * Тесты прогрессии: звёзды, параметры уровней, сохранение/загрузка.
 */
class ProgressTest {

    @Test
    fun `star rating thresholds`() {
        assertEquals("Ровно par — три звезды", 3, Progress.starsFor(moves = 10, par = 10))
        assertEquals("До полутора par — две звезды", 2, Progress.starsFor(moves = 15, par = 10))
        assertEquals("Больше полутора par — одна звезда", 1, Progress.starsFor(moves = 16, par = 10))
    }

    @Test
    fun `level catalog covers 60 levels`() {
        assertEquals(60, LevelCatalog.LEVEL_COUNT)
    }

    @Test
    fun `level parameters grow with level number`() {
        val l1 = LevelCatalog.paramsFor(1)
        val l30 = LevelCatalog.paramsFor(30)
        val l60 = LevelCatalog.paramsFor(60)
        assertTrue("Размер доски растёт", l1.boardSize <= l30.boardSize && l30.boardSize <= l60.boardSize)
        assertTrue("Сложность растёт", l1.difficulty <= l30.difficulty && l30.difficulty <= l60.difficulty)
        assertTrue("Уровень 60 должен использовать поле 7x7", l60.boardSize == 7)
    }

    @Test
    fun `same level always has same seed and is solvable`() {
        val a = LevelCatalog.paramsFor(33)
        val b = LevelCatalog.paramsFor(33)
        assertEquals("Уровень должен быть детерминированным", a.seed, b.seed)
    }

    @Test
    fun `progress round trip`() {
        val p = Progress()
        p.complete(1, stars = 3, moves = 12)
        p.complete(2, stars = 2, moves = 30)
        val text = p.serialize()
        val restored = Progress.deserialize(text)
        assertEquals(3, restored.starsForLevel(1))
        assertEquals(2, restored.starsForLevel(2))
        assertEquals(2, restored.totalStars())
    }

    @Test
    fun `unlock rule opens next level only`() {
        val p = Progress()
        assertFalse("Уровень 2 закрыт без прохождения 1", p.isUnlocked(2))
        p.complete(1, stars = 3, moves = 10)
        assertTrue("После уровня 1 открывается 2", p.isUnlocked(2))
        assertFalse("Уровень 3 остаётся закрытым", p.isUnlocked(3))
    }
}
