package petclicker;

import org.junit.Test;
import static org.junit.Assert.*;
import com.petclicker.game.PetGame;
import com.petclicker.game.EventSpawner;

/**
 * Тесты кликера с питомцами (ось C: ивент-спавнер и цепочки комбо).
 */
public class PetEconomyTest {

    @Test
    public void tap_gives_coins() {
        PetGame g = new PetGame();
        g.tap();
        assertTrue("Тап даёт монеты", g.coins() > 0);
    }

    @Test
    public void combo_chain_multiplies_coins() {
        PetGame g = new PetGame();
        double single = g.tapValue(0);
        double chained = g.tapValue(9);
        assertTrue("Комбо множит награду", chained > single);
    }

    @Test
    public void combo_resets_after_timeout() {
        PetGame g = new PetGame();
        g.tap();
        g.tap();
        assertEquals("Комбо растёт", 2, g.combo());
        g.tick(2.0);
        assertEquals("Комбо сбрасывается по таймауту", 0, g.combo());
    }

    @Test
    public void pets_produce_automatically() {
        PetGame g = new PetGame();
        g.addCoins(1000);
        g.buyPet(0);
        double before = g.coins();
        g.tick(10.0);
        assertTrue("Питомцы приносят монеты со временем", g.coins() > before);
    }

    @Test
    public void pet_cost_grows() {
        PetGame g = new PetGame();
        double c1 = g.petCost(0);
        g.addCoins(1e9);
        g.buyPet(0);
        double c2 = g.petCost(0);
        assertTrue("Цена питомца растёт", c2 > c1);
    }

    @Test
    public void buying_pet_requires_coins() {
        PetGame g = new PetGame();
        assertFalse("Нет монет — нет покупки", g.buyPet(0));
        assertEquals(0, g.petCount(0));
    }

    @Test
    public void events_spawn_deterministically() {
        var a = EventSpawner.generate(42, 10);
        var b = EventSpawner.generate(42, 10);
        assertEquals("Ивенты детерминированы", a.size(), b.size());
        for (int i = 0; i < a.size(); i++)
            assertEquals(a.get(i).timeSeconds, b.get(i).timeSeconds, 1e-9);
    }

    @Test
    public void events_include_golden_paw() {
        var events = EventSpawner.generate(7, 30);
        boolean anyGolden = events.stream().anyMatch(e -> e.type == EventSpawner.Type.GOLDEN_PAW);
        assertTrue("В серии ивентов есть золотая лапка", anyGolden);
    }

    @Test
    public void golden_paw_grants_bonus() {
        PetGame g = new PetGame();
        double before = g.coins();
        g.triggerEvent(EventSpawner.Type.GOLDEN_PAW);
        assertTrue("Золотая лапка даёт бонус", g.coins() > before);
    }

    @Test
    public void save_load_round_trip() {
        PetGame g = new PetGame();
        g.addCoins(5000);
        g.buyPet(1);
        g.tap();
        String save = g.save();
        PetGame restored = PetGame.load(save);
        assertEquals("Монеты сохранены", g.coins(), restored.coins(), 1e-6);
        assertEquals("Питомцы сохранены", g.petCount(1), restored.petCount(1));
    }

    @Test
    public void levels_unlock_pets() {
        assertFalse("Питомец 3 закрыт на 1 уровне", PetGame.isPetUnlocked(2, 1));
        assertTrue("Питомец 3 открывается на 5 уровне", PetGame.isPetUnlocked(2, 5));
    }
}
