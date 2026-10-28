package neonidle;

import org.junit.Test;
import static org.junit.Assert.*;
import com.neonidle.game.Game;

/**
 * Тесты системы престижа (ось C: кривая престижа как генератор долгосрочной цели).
 */
public class PrestigeTest {

    @Test
    public void shards_grow_with_total_earned() {
        long s1 = Game.shardsFor(1e9);
        long s2 = Game.shardsFor(1e12);
        long s3 = Game.shardsFor(1e15);
        assertTrue("Осколки растут с заработком", s2 > s1 && s3 > s2);
    }

    @Test
    public void shards_below_threshold_are_zero() {
        assertEquals("До порога осколков нет", 0, Game.shardsFor(1e6));
    }

    @Test
    public void prestige_resets_progress_but_keeps_shards() {
        Game g = new Game();
        g.addEnergy(1e18);
        g.buyGenerator(0);
        g.buyUpgrade(0);
        long shards = Game.shardsFor(g.totalEarned());
        assertTrue("Осколки должны начисляться", shards > 0);
        g.prestige();
        assertEquals("Энергия обнулена", 0.0, g.energy(), 1e-6);
        assertEquals("Генераторы обнулены", 0, g.generatorCount(0));
        assertEquals("Осколки сохранены", shards, g.shards());
        assertEquals("Счётчик престижа вырос", 1, g.prestigeCount());
    }

    @Test
    public void shards_boost_production() {
        Game plain = new Game();
        plain.addEnergy(1e12);
        plain.buyGenerator(0);

        Game boosted = new Game();
        boosted.addEnergy(1e18);
        boosted.buyGenerator(0);
        boosted.prestige();
        boosted.addEnergy(1e12);
        boosted.buyGenerator(0);

        assertTrue("Осколки дают множитель производства",
                boosted.energyPerSecond() > plain.energyPerSecond());
    }

    @Test
    public void prestige_not_available_below_threshold() {
        Game g = new Game();
        assertFalse("Престиж недоступен без прогресса", g.canPrestige());
    }
}
