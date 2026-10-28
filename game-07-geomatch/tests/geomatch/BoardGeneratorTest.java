package geomatch;

import org.junit.Test;
import static org.junit.Assert.*;
import com.geomatch.game.MatchBoard;
import com.geomatch.game.MatchLogic;

/**
 * Тесты генератора доски match-3 (ось C: генератор доски с проверкой на тупики).
 */
public class BoardGeneratorTest {

    @Test
    public void initialBoard_hasNoMatches() {
        for (long seed = 1; seed <= 20; seed++) {
            MatchBoard board = new MatchBoard(8, seed);
            assertTrue("Стартовая доска без готовых совпадений (seed=" + seed + ")",
                    MatchLogic.findMatches(board.grid()).isEmpty());
        }
    }

    @Test
    public void initialBoard_hasAvailableMove() {
        for (long seed = 1; seed <= 20; seed++) {
            MatchBoard board = new MatchBoard(8, seed);
            assertTrue("На доске есть хотя бы один ход (seed=" + seed + ")",
                    board.hasAvailableMove());
        }
    }

    @Test
    public void generation_isDeterministic() {
        MatchBoard a = new MatchBoard(8, 42);
        MatchBoard b = new MatchBoard(8, 42);
        assertTrue("Генератор детерминирован", java.util.Arrays.deepEquals(a.grid(), b.grid()));
    }

    @Test
    public void differentSeeds_differentBoards() {
        MatchBoard a = new MatchBoard(8, 1);
        MatchBoard b = new MatchBoard(8, 2);
        assertFalse("Разные сиды дают разные доски",
                java.util.Arrays.deepEquals(a.grid(), b.grid()));
    }

    @Test
    public void colors_withinRange() {
        MatchBoard board = new MatchBoard(8, 5);
        for (int[] row : board.grid())
            for (int c : row)
                assertTrue("Цвет вне диапазона: " + c, c >= 0 && c < MatchBoard.COLORS);
    }

    @Test
    public void swap_withoutMatch_isRejected() {
        MatchBoard board = new MatchBoard(8, 3);
        // Находим заведомо бессмысленный обмен: пробуем все пары, часть точно не даст матч
        boolean anyRejected = false;
        for (int y = 0; y < 8 && !anyRejected; y++) {
            for (int x = 0; x < 7; x++) {
                if (!board.swap(x, y, x + 1, y)) {
                    anyRejected = true;
                    break;
                }
            }
        }
        assertTrue("Бессмысленный обмен должен быть отклонён", anyRejected);
    }

    @Test
    public void gravity_refillsEmptyCells() {
        MatchBoard board = new MatchBoard(8, 9);
        board.clearCell(3, 3);
        board.applyGravityAndRefill(123L);
        for (int[] row : board.grid())
            for (int c : row)
                assertTrue("После падения пустых клеток нет", c >= 0);
    }
}
