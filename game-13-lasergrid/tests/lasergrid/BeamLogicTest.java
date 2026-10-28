package lasergrid;

import org.junit.Test;
import static org.junit.Assert.*;
import com.lasergrid.game.BeamLogic;
import com.lasergrid.game.PuzzleState;

/**
 * Тесты трассировки луча (правила отражения зеркал) — основа головоломки.
 */
public class BeamLogicTest {

    @Test
    public void beam_travels_straight_without_mirrors() {
        char[][] grid = {
                {'#', '#', '#', '#', '#'},
                {'#', 'S', '.', '.', 'T'},
                {'#', '#', '#', '#', '#'}
        };
        boolean[][] lit = BeamLogic.trace(grid);
        assertTrue("Луч доходит до цели по прямой", lit[1][4]);
    }

    @Test
    public void beam_reflects_off_slash_mirror() {
        // '/' отражает луч, идущий вправо, вверх: цель стоит над зеркалом
        char[][] grid = {
                {'#', '#', '#', '#', '#'},
                {'#', '.', 'T', '.', '#'},
                {'#', 'S', '/', '.', '#'},
                {'#', '#', '#', '#', '#'}
        };
        boolean[][] lit = BeamLogic.trace(grid);
        assertTrue("Луч отразился вверх от '/'", lit[1][2]);
    }

    @Test
    public void beam_reflects_off_backslash_mirror() {
        // '\' отражает луч, идущий вправо, вниз — в стену; цель слева сверху не освещена
        char[][] grid = {
                {'#', '#', '#', '#', '#'},
                {'#', 'T', '.', '.', '#'},
                {'#', 'S', '\\', '.', '#'},
                {'#', '#', '#', '#', '#'}
        };
        boolean[][] lit = BeamLogic.trace(grid);
        assertFalse("Луч ушёл вниз от '\\' и не осветил цель", lit[1][1]);
    }

    @Test
    public void walls_block_beam() {
        char[][] grid = {
                {'#', '#', '#', '#', '#'},
                {'#', 'S', '#', '.', 'T'},
                {'#', '#', '#', '#', '#'}
        };
        boolean[][] lit = BeamLogic.trace(grid);
        assertFalse("Стена блокирует луч", lit[1][4]);
    }

    @Test
    public void all_targets_must_be_lit_for_solved() {
        char[][] grid = {
                {'#', '#', '#', '#', '#', '#'},
                {'#', 'S', '.', '.', 'T', '#'},
                {'#', '#', '#', '#', '.', '#'},
                {'#', 'T', '.', '.', '.', '#'},
                {'#', '#', '#', '#', '#', '#'}
        };
        assertFalse("Не все цели освещены", BeamLogic.allTargetsLit(grid));
    }

    @Test
    public void beam_from_source_only() {
        char[][] grid = {
                {'#', '#', '#'},
                {'#', '.', '#'},
                {'#', '#', '#'}
        };
        boolean[][] lit = BeamLogic.trace(grid);
        assertFalse("Без источника луча нет", lit[1][1]);
    }
}
