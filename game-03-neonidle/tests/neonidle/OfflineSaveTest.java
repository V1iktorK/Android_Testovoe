package neonidle;

import org.junit.Test;
import static org.junit.Assert.*;
import com.neonidle.game.Game;

/**
 * Тесты офлайн-прогресса и сохранений.
 * (Ось C: офлайн-прогресс с ограничением по времени — отдельная часть баланса.)
 */
public class OfflineSaveTest {

    @Test
    public void offline_gain_is_production_times_time() {
        Game g = new Game();
        g.addEnergy(1e12);
        g.buyGenerator(0);
        double rate = g.energyPerSecond();
        double gain = g.offlineGain(3600); // час
        assertEquals("Офлайн-начисление = производство * время", rate * 3600, gain, 1e-3);
    }

    @Test
    public void offline_gain_is_capped() {
        Game g = new Game();
        g.addEnergy(1e12);
        g.buyGenerator(0);
        double gain8h = g.offlineGain(8 * 3600);
        double gain999h = g.offlineGain(999 * 3600);
        assertEquals("Офлайн ограничен 8 часами", gain8h, gain999h, 1e-6);
    }

    @Test
    public void save_load_round_trip() {
        Game g = new Game();
        g.addEnergy(1e15);
        g.buyGenerator(0);
        g.buyGenerator(1);
        g.buyUpgrade(0);
        g.tick(10);
        String save = g.save();
        Game restored = Game.load(save);
        assertEquals("Энергия сохранена", g.energy(), restored.energy(), 1e-3);
        assertEquals("Генераторы сохранены", g.generatorCount(0), restored.generatorCount(0));
        assertEquals("Производство восстановлено", g.energyPerSecond(), restored.energyPerSecond(), 1e-6);
    }

    @Test
    public void load_bad_save_returns_fresh_game() {
        Game g = Game.load("мусор|не|формат");
        assertEquals("Новый инстанс при ошибке загрузки", 0.0, g.energy(), 1e-9);
        assertEquals(0, g.prestigeCount());
    }

    @Test
    public void achievements_unlock_by_milestones() {
        Game g = new Game();
        assertEquals("До старта достижений нет", 0, g.achievements().size());
        g.addEnergy(1e6);
        g.checkAchievements();
        assertTrue("Первое достижение за энергию", g.achievements().size() > 0);
    }
}
