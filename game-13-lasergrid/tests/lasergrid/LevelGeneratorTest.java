package lasergrid;

import org.junit.Test;
import static org.junit.Assert.*;
import com.lasergrid.game.LevelGenerator;
import com.lasergrid.game.PuzzleState;

/**
 * Тесты solution-first генератора (ось C: уровни строятся от решения).
 */
public class LevelGeneratorTest {

    @Test
    public void generated_level_is_solvable() {
        for (long seed = 1; seed <= 15; seed++) {
            PuzzleState p = LevelGenerator.generate(seed, 2);
            assertTrue("Уровень решаем в эталонной раскладке (seed=" + seed + ")",
                    p.isSolvedInReferenceState());
        }
    }

    @Test
    public void generation_is_deterministic() {
        PuzzleState a = LevelGenerator.generate(42, 3);
        PuzzleState b = LevelGenerator.generate(42, 3);
        assertEquals("Генератор детерминирован", a.signature(), b.signature());
    }

    @Test
    public void scrambled_level_starts_unsolved() {
        int scrambled = 0;
        for (long seed = 1; seed <= 20; seed++) {
            PuzzleState p = LevelGenerator.generate(seed, 3);
            if (!p.isSolved()) scrambled++;
        }
        assertTrue("Большинство уровней стартуют нерешёнными", scrambled >= 16);
    }

    @Test
    public void difficulty_adds_mirrors() {
        int easyTotal = 0;
        int hardTotal = 0;
        for (long seed = 1; seed <= 10; seed++) {
            easyTotal += LevelGenerator.generate(seed, 1).mirrorCount();
            hardTotal += LevelGenerator.generate(seed, 6).mirrorCount();
        }
        assertTrue("Сложность в среднем добавляет зеркала (легко=" + easyTotal + ", сложно=" + hardTotal + ")",
                hardTotal > easyTotal);
    }

    @Test
    public void par_moves_is_positive() {
        for (long seed = 1; seed <= 12; seed++) {
            PuzzleState p = LevelGenerator.generate(seed, 2);
            assertTrue("par > 0 (seed=" + seed + ")", p.parMoves() > 0);
        }
    }

    @Test
    public void rotate_changes_state() {
        PuzzleState p = LevelGenerator.generate(5, 2);
        String before = p.signature();
        int idx = p.firstUnsolvedMirror();
        assertTrue("Есть неправильное зеркало", idx >= 0);
        p.rotate(idx);
        assertNotEquals("Поворот меняет состояние", before, p.signature());
    }

    @Test
    public void stars_by_moves() {
        assertEquals(3, PuzzleState.starsFor(4, 4));
        assertEquals(2, PuzzleState.starsFor(6, 4));
        assertEquals(1, PuzzleState.starsFor(12, 4));
    }
}
