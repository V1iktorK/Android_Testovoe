package farm;

import org.junit.Test;
import static org.junit.Assert.*;
import com.farm.game.CropCatalog;
import com.farm.game.FarmPlot;
import com.farm.game.BreedingBook;

/**
 * Тесты фермы (ось C: матрица наследования культур при скрещивании).
 */
public class FarmCoreTest {

    @Test
    public void crops_grow_over_time() {
        FarmPlot plot = new FarmPlot(CropCatalog.get("wheat"));
        plot.tick(1.0);
        assertTrue("Культура растёт", plot.growth() > 0);
    }

    @Test
    public void crops_ripen_at_maturity() {
        FarmPlot plot = new FarmPlot(CropCatalog.get("wheat"));
        for (int i = 0; i < 100 && !plot.isRipe(); i++) plot.tick(1.0);
        assertTrue("Культура созревает", plot.isRipe());
    }

    @Test
    public void harvest_gives_yield_once() {
        FarmPlot plot = new FarmPlot(CropCatalog.get("wheat"));
        for (int i = 0; i < 100 && !plot.isRipe(); i++) plot.tick(1.0);
        int yield = plot.harvest();
        assertTrue("Урожай получен", yield > 0);
        assertEquals("Повторный сбор невозможен", 0, plot.harvest());
    }

    @Test
    public void watering_speeds_growth() {
        FarmPlot dry = new FarmPlot(CropCatalog.get("wheat"));
        FarmPlot wet = new FarmPlot(CropCatalog.get("wheat"));
        wet.water();
        dry.tick(1.0);
        wet.tick(1.0);
        assertTrue("Полив ускоряет рост", wet.growth() > dry.growth());
    }

    @Test
    public void breeding_deterministic() {
        var a = BreedingBook.breed("wheat", "berry", 42);
        var b = BreedingBook.breed("wheat", "berry", 42);
        assertEquals("Скрещивание детерминировано", a, b);
    }

    @Test
    public void breeding_produces_known_crop() {
        var result = BreedingBook.breed("wheat", "berry", 7);
        assertTrue("Итог скрещивания — известная культура",
                CropCatalog.get(result) != null);
    }

    @Test
    public void breeding_matrix_is_symmetric_in_result() {
        var ab = BreedingBook.breed("wheat", "berry", 11);
        var ba = BreedingBook.breed("berry", "wheat", 11);
        assertEquals("Матрица симметрична", ab, ba);
    }

    @Test
    public void rare_crops_breed_rare_results() {
        var result = BreedingBook.breed("starfruit", "starfruit", 3);
        assertTrue("Редкий родитель — редкий результат",
                CropCatalog.get(result).rarity >= 3);
    }

    @Test
    public void catalog_crops_have_valid_stats() {
        for (String name : CropCatalog.names()) {
            var crop = CropCatalog.get(name);
            assertTrue("Время роста > 0", crop.growSeconds > 0);
            assertTrue("Урожай > 0", crop.yieldAmount > 0);
        }
    }

    @Test
    public void sell_price_scales_with_rarity() {
        var common = CropCatalog.get("wheat");
        var rare = CropCatalog.get("starfruit");
        assertTrue("Редкие культуры дороже", rare.sellPrice > common.sellPrice);
    }
}
