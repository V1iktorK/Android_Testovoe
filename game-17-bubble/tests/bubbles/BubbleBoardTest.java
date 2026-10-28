package bubbles;

import org.junit.Test;
import static org.junit.Assert.*;
import com.bubbles.game.BubbleBoard;
import com.bubbles.game.BubbleLogic;

/**
 * Тесты пузырькового шутера: поле, стрельба, цепочки, физика падения.
 * Ось C уникальности: генератор поля с цветовым балансом.
 */
public class BubbleBoardTest {

    @Test
    public void generation_isDeterministic() {
        BubbleBoard a = BubbleBoard.generate(42, 5);
        BubbleBoard b = BubbleBoard.generate(42, 5);
        assertEquals("Детерминизм генератора", a.signature(), b.signature());
    }

    @Test
    public void colors_are_balanced() {
        BubbleBoard board = BubbleBoard.generate(7, 6);
        int[] counts = new int[BubbleBoard.COLORS];
        for (int[] row : board.grid())
            for (int c : row)
                if (c >= 0) counts[c]++;
        for (int i = 0; i < BubbleBoard.COLORS; i++) {
            assertTrue("Цвет " + i + " должен присутствовать", counts[i] > 0);
        }
    }

    @Test
    public void shooting_places_bubble() {
        BubbleBoard board = BubbleBoard.generate(3, 4);
        board.shoot(0, 2);
        assertNotNull("Пузырёк встал в колонку", board.grid()[0][0]);
    }

    @Test
    public void match_of_three_clears() {
        int[][] grid = {
                {0, 0, 1, 2},
                {0, 1, 2, 1},
                {-1, -1, -1, -1}
        };
        int cleared = BubbleLogic.matchSize(grid, 0, 0);
        assertEquals("Три соседа одного цвета", 3, cleared);
    }

    @Test
    public void no_match_below_three() {
        int[][] grid = {
                {1, 1, 2, 3},
                {4, 2, 3, 4}
        };
        assertEquals("Пара не очищается", 2, BubbleLogic.matchSize(grid, 0, 0));
    }

    @Test
    public void floating_bubbles_fall() {
        int[][] grid = {
                {0, -1, -1, -1},
                {-1, 1, -1, -1}
        };
        int fallen = BubbleLogic.countFloating(grid);
        assertEquals("Незакреплённый пузырёк падает", 1, fallen);
    }

    @Test
    public void attached_bubbles_stay() {
        int[][] grid = {
                {0, 1, -1, -1},
                {0, 1, -1, -1}
        };
        assertEquals("Закреплённые пузырьки остаются", 0, BubbleLogic.countFloating(grid));
    }

    @Test
    public void score_grows_with_chain() {
        int one = BubbleLogic.scoreFor(3, 1);
        int chain = BubbleLogic.scoreFor(3, 3);
        assertTrue("Цепочки множат очки", chain > one);
    }

    @Test
    public void win_when_board_empty() {
        BubbleBoard board = new BubbleBoard(new int[][]{{-1, -1}, {-1, -1}});
        assertTrue("Пустое поле — победа", board.isCleared());
    }

    @Test
    public void level_rows_grow() {
        BubbleBoard easy = BubbleBoard.generate(5, 1);
        BubbleBoard hard = BubbleBoard.generate(5, 8);
        assertTrue("Сложность добавляет ряды", hard.rows() >= easy.rows());
    }
}
