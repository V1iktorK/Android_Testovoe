package neonidle;

import org.junit.Test;
import static org.junit.Assert.*;
import com.neonidle.game.Game;
import com.neonidle.game.GeneratorDef;

/**
 * Тесты экономики кликера: кривая стоимости, покупка генераторов, производство.
 * (Ось C: параметрическая кривая баланса — стоимость растёт геометрически.)
 */
public class EconomyTest {

    @Test
    public void cost_curve_is_geometric() {
        GeneratorDef d = Game.generators()[0];
        double c0 = d.costFor(0);
        double c1 = d.costFor(1);
        double c2 = d.costFor(2);
        assertEquals("Стоимость растёт по геометрической прогрессии", c1 / c0, c2 / c1, 1e-9);
        assertTrue("Стоимость второго генератора выше", c1 > c0);
    }

    @Test
    public void purchase_requires_energy_and_adds_production() {
        Game g = new Game();
        double before = g.energyPerSecond();
        g.addEnergy(Game.generators()[0].costFor(0));
        boolean bought = g.buyGenerator(0);
        assertTrue("Покупка при хватке энергии успешна", bought);
        assertTrue("Производство выросло", g.energyPerSecond() > before);
        assertEquals("Энергия списана", 0.0, g.energy(), 1e-6);
    }

    @Test
    public void purchase_fails_without_energy() {
        Game g = new Game();
        assertFalse("Покупка без энергии невозможна", g.buyGenerator(0));
        assertEquals("Состояние не изменилось", 0, g.generatorCount(0));
    }

    @Test
    public void production_scales_with_count() {
        Game g = new Game();
        g.addEnergy(1e12);
        g.buyGenerator(0);
        double one = g.energyPerSecond();
        g.buyGenerator(0);
        assertEquals("Производство линейно по числу генераторов", one * 2, g.energyPerSecond(), 1e-6);
    }

    @Test
    public void click_value_grows_with_upgrades() {
        Game g = new Game();
        double base = g.clickValue();
        g.addEnergy(1e15);
        g.buyUpgrade(0); // апгрейд клика
        assertTrue("Апгрейд увеличивает силу клика", g.clickValue() > base);
    }

    @Test
    public void tick_accumulates_energy() {
        Game g = new Game();
        g.addEnergy(1e12);
        g.buyGenerator(0);
        double before = g.energy();
        g.tick(1.0);
        assertEquals("За секунду начисляется производство", before + g.energyPerSecond(), g.energy(), 1e-3);
    }

    @Test
    public void format_numbers_is_stable() {
        assertEquals("1.5K", Game.formatNumber(1500));
        assertEquals("2.0M", Game.formatNumber(2_000_000));
        assertEquals("1.0B", Game.formatNumber(1e9));
    }
}
