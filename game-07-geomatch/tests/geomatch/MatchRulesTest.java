package geomatch;

import org.junit.Test;
import static org.junit.Assert.*;
import com.geomatch.game.MatchBoard;
import com.geomatch.game.MatchLogic;
import com.geomatch.game.LevelCatalog;
import com.geomatch.game.GameSession;

/**
 * Тесты правил match-3: поиск совпадений, каскады, спец-плитки, скоринг, уровни.
 */
public class MatchRulesTest {

    @Test
    public void findMatches_detectsHorizontalRun() {
        int[][] grid = {
            {0, 0, 0, 1},
            {1, 2, 1, 2},
            {2, 1, 2, 1},
            {1, 2, 1, 2}
        };
        assertEquals("Три подряд по горизонтали", 3, MatchLogic.findMatches(grid).size());
    }

    @Test
    public void findMatches_detectsVerticalRun() {
        int[][] grid = {
            {0, 1, 2, 1},
            {0, 2, 1, 2},
            {0, 1, 2, 1},
            {1, 2, 1, 2}
        };
        assertEquals("Три подряд по вертикали", 3, MatchLogic.findMatches(grid).size());
    }

    @Test
    public void findMatches_ignoresPairs() {
        int[][] grid = {
            {0, 0, 1, 2},
            {1, 2, 2, 1},
            {2, 1, 1, 2},
            {1, 2, 2, 1}
        };
        assertTrue("Пара — не совпадение", MatchLogic.findMatches(grid).isEmpty());
    }

    @Test
    public void scoring_cascadeMultiplierGrows() {
        int base = MatchLogic.scoreFor(3, 0);
        int cascade = MatchLogic.scoreFor(3, 2);
        assertTrue("Каскад множит очки", cascade > base);
    }

    @Test
    public void matchFour_createsSpecialTile() {
        int[][] grid = {
            {0, 0, 0, 0, 1},
            {1, 2, 1, 2, 1},
            {2, 1, 2, 1, 2},
            {1, 2, 1, 2, 1},
            {2, 1, 2, 1, 2}
        };
        assertEquals("Четыре подряд дают спец-плитку",
                MatchLogic.Special.LINE_HORIZONTAL, MatchLogic.specialForRun(4));
    }

    @Test
    public void specialForRun_thresholds() {
        assertEquals(MatchLogic.Special.NONE, MatchLogic.specialForRun(3));
        assertEquals(MatchLogic.Special.LINE_VERTICAL, MatchLogic.specialForRun(5));
    }

    @Test
    public void levelCatalog_growingDifficulty() {
        LevelCatalog.Params l1 = LevelCatalog.paramsFor(1);
        LevelCatalog.Params l20 = LevelCatalog.paramsFor(20);
        LevelCatalog.Params l40 = LevelCatalog.paramsFor(40);
        assertTrue("Ходов на сложных уровнях не больше", l20.moves <= l1.moves);
        assertTrue("Цель растёт", l40.targetScore > l1.targetScore);
    }

    @Test
    public void stars_byScoreRatio() {
        assertEquals(3, GameSession.starsFor(1000, 1000));
        assertEquals(2, GameSession.starsFor(700, 1000));
        assertEquals(1, GameSession.starsFor(300, 1000));
    }

    @Test
    public void moveLimit_endsGame() {
        GameSession session = new GameSession(5, 1000, 0);
        session.useMove();
        assertTrue("Ходы расходуются", session.movesLeft() == 4);
        for (int i = 0; i < 4; i++) session.useMove();
        assertFalse("После расхода ходов игра окончена", session.hasMovesLeft());
    }
}
