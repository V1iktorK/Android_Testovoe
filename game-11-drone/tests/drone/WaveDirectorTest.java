package drone;

import org.junit.Test;
import static org.junit.Assert.*;
import com.drone.game.WaveDirector;
import com.drone.game.CombatMath;

/**
 * Тесты «режиссёра волн» (ось C: адаптивная генерация волн по обратной связи от игрока).
 */
public class WaveDirectorTest {

    @Test
    public void wave_generation_isDeterministic() {
        var a = WaveDirector.generate(42, 3, 1.0);
        var b = WaveDirector.generate(42, 3, 1.0);
        assertEquals("Число врагов совпадает", a.size(), b.size());
        for (int i = 0; i < a.size(); i++)
            assertEquals(a.get(i).type, b.get(i).type);
    }

    @Test
    public void wave_size_grows_with_wave_number() {
        var w3 = WaveDirector.generate(7, 3, 1.0);
        var w9 = WaveDirector.generate(7, 9, 1.0);
        assertTrue("Врагов больше на поздних волнах", w9.size() >= w3.size());
    }

    @Test
    public void harder_performance_scales_enemies() {
        var calm = WaveDirector.generate(5, 6, 0.5);
        var hot = WaveDirector.generate(5, 6, 1.5);
        float calmHp = calm.stream().map(e -> e.hp).reduce(0f, Float::sum);
        float hotHp = hot.stream().map(e -> e.hp).reduce(0f, Float::sum);
        assertTrue("Сильный игрок получает более крепких врагов", hotHp >= calmHp);
    }

    @Test
    public void enemy_types_within_palette() {
        var wave = WaveDirector.generate(11, 12, 1.2);
        for (var e : wave) {
            assertTrue("Неизвестный тип: " + e.type,
                    e.type == WaveDirector.Type.Scout || e.type == WaveDirector.Type.Tank
                            || e.type == WaveDirector.Type.Sprinter || e.type == WaveDirector.Type.Turret);
        }
    }

    @Test
    public void boss_waves_contain_boss() {
        var wave = WaveDirector.generate(3, 5, 1.0);
        boolean boss = wave.stream().anyMatch(e -> e.type == WaveDirector.Type.Boss);
        assertTrue("Каждая 5-я волна — босс", boss);
    }

    @Test
    public void performance_metric_is_clamped() {
        assertEquals(0.5, WaveDirector.clampPerformance(-5), 1e-9);
        assertEquals(1.5, WaveDirector.clampPerformance(99), 1e-9);
    }
}
