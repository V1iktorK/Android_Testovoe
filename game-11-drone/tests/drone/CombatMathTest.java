package drone;

import org.junit.Test;
import static org.junit.Assert.*;
import com.drone.game.CombatMath;
import com.drone.game.DroneState;
import com.drone.game.WaveDirector;

/**
 * Тесты боевой математики дрона: урон, перезарядка, столкновения, апгрейды.
 */
public class CombatMathTest {

    @Test
    public void damage_respects_armor() {
        float dmg = CombatMath.damageAfterArmor(50f, 20f);
        assertEquals(30f, dmg, 0.001f);
        assertEquals("Урон не ниже нуля", 0f, CombatMath.damageAfterArmor(10f, 20f), 0.001f);
    }

    @Test
    public void fire_rate_limits_shots() {
        DroneState d = new DroneState();
        d.fireRate = 2f; // 2 выстрела в секунду
        int shots = 0;
        for (float t = 0; t < 1f; t += 0.1f) {
            if (d.canFireAt(t)) {
                d.fire(t);
                shots++;
            }
        }
        assertTrue("За секунду не больше 2–3 выстрелов", shots <= 3);
    }

    @Test
    public void bullet_hit_detection() {
        assertTrue("Пуля в радиусе попадает", CombatMath.hits(0f, 0f, 3f, 4f, 6f));
        assertFalse("Пуля вне радиуса промахивается", CombatMath.hits(0f, 0f, 3f, 4f, 4f));
    }

    @Test
    public void shield_absorbs_damage() {
        DroneState d = new DroneState();
        d.shield = 30f;
        float taken = d.takeDamage(45f);
        assertEquals("Щит поглотил часть урона", 15f, taken, 0.001f);
        assertEquals("Щит истощён", 0f, d.shield, 0.001f);
    }

    @Test
    public void upgrade_costs_grow() {
        float c1 = CombatMath.upgradeCost(0);
        float c2 = CombatMath.upgradeCost(1);
        float c3 = CombatMath.upgradeCost(5);
        assertTrue("Цена апгрейда растёт", c2 > c1 && c3 > c2);
    }

    @Test
    public void upgrades_boost_damage() {
        DroneState d = new DroneState();
        float base = d.bulletDamage();
        d.upgradeDamage();
        assertTrue("Апгрейд усиливает урон", d.bulletDamage() > base);
    }

    @Test
    public void score_for_wave() {
        int s1 = CombatMath.scoreFor(3, 10);
        int s2 = CombatMath.scoreFor(4, 10);
        assertTrue("Очки растут с волной", s2 > s1);
    }

    @Test
    public void player_performance_reflects_health() {
        assertEquals(1.0, WaveDirector.clampPerformance(1.0), 1e-9);
        assertTrue("Плохая игра снижает сложность", WaveDirector.clampPerformance(0.2) < 1.0);
    }
}
