package runner;

import org.junit.Test;
import static org.junit.Assert.*;
import com.runner.game.LaneState;
import com.runner.game.RunRules;

/**
 * Тесты правил раннера: полосы, свайпы, столкновения, монеты, достижения дистанции.
 */
public class RunRulesTest {

    @Test
    public void laneSwitch_clampsToThreeLanes() {
        LaneState lane = new LaneState();
        assertEquals(1, lane.getCurrent());
        lane.swipeLeft();
        assertEquals(0, lane.getCurrent());
        lane.swipeLeft();
        assertEquals("Полоса ограничена снизу", 0, lane.getCurrent());
        lane.swipeRight();
        lane.swipeRight();
        assertEquals("Полоса ограничена сверху", 2, lane.getCurrent());
    }

    @Test
    public void jump_and_roll_states() {
        LaneState lane = new LaneState();
        lane.swipeUp();
        assertTrue("После свайпа вверх — прыжок", lane.isJumping());
        assertFalse("В прыжке нельзя катиться", lane.isRolling());
        lane.swipeDown();
        assertFalse("После свайпа вниз прыжок заканчивается", lane.isJumping());
        assertTrue("После свайпа вниз — кувырок", lane.isRolling());
    }

    @Test
    public void collision_highObstacle_hitsUnlessJumping() {
        assertTrue("В прыжке высокое препятствие безопасно",
                !RunRules.collides('h', true, false));
        assertTrue("Без прыжка высокое препятствие бьёт",
                RunRules.collides('h', false, false));
    }

    @Test
    public void collision_lowObstacle_hitsUnlessRolling() {
        assertTrue("В кувырке низкий барьер безопасен",
                !RunRules.collides('l', false, true));
        assertTrue("Без кувырка низкий барьер бьёт",
                RunRules.collides('l', false, false));
    }

    @Test
    public void collision_wall_alwaysHits() {
        assertTrue("Стена бьёт даже в прыжке", RunRules.collides('x', true, true));
    }

    @Test
    public void coins_grow_score() {
        int base = RunRules.scoreFor(1000, 0);
        int withCoins = RunRules.scoreFor(1000, 10);
        assertEquals("Монета = 10 очков", base + 100, withCoins);
    }

    @Test
    public void speed_grows_with_distance() {
        float s1 = RunRules.speedAt(0);
        float s2 = RunRules.speedAt(1000);
        float s3 = RunRules.speedAt(10000);
        assertTrue("Скорость растёт", s2 > s1 && s3 > s2);
        assertTrue("Есть предел скорости", s3 <= RunRules.MAX_SPEED);
    }

    @Test
    public void characters_unlock_by_distance() {
        assertFalse("Второй герой закрыт на старте", RunRules.isUnlocked(2, 0));
        assertTrue("Второй герой открывается на 2000 м", RunRules.isUnlocked(2, 2000));
        assertTrue("Третий герой открывается на 5000 м", RunRules.isUnlocked(3, 5000));
    }
}
