package wasteland;

import org.junit.Test;
import static org.junit.Assert.*;
import com.wasteland.game.LootTable;
import com.wasteland.game.SurvivalState;
import com.wasteland.game.CraftingBook;

/**
 * Тесты выживания (ось C: лут-таблицы + крафт-дерево + цикл дня и ночи).
 */
public class SurvivalCoreTest {

    @Test
    public void loot_isDeterministic() {
        var a = LootTable.generate(42, "hospital", 3);
        var b = LootTable.generate(42, "hospital", 3);
        assertEquals("Лут детерминирован", a.size(), b.size());
        for (int i = 0; i < a.size(); i++)
            assertEquals(a.get(i).item, b.get(i).item);
    }

    @Test
    public void loot_richer_on_rare_spots() {
        var common = LootTable.generate(7, "street", 1);
        var rare = LootTable.generate(7, "military", 1);
        int commonValue = common.stream().mapToInt(d -> d.value).sum();
        int rareValue = rare.stream().mapToInt(d -> d.value).sum();
        assertTrue("Редкие локации дают более ценный лут", rareValue > commonValue);
    }

    @Test
    public void loot_items_are_known() {
        var loot = LootTable.generate(11, "hospital", 5);
        for (var d : loot) {
            assertTrue("Неизвестный предмет: " + d.item,
                    LootTable.KNOWN_ITEMS.contains(d.item));
        }
    }

    @Test
    public void crafting_consumes_ingredients() {
        CraftingBook book = new CraftingBook();
        assertTrue("Рецепт костра требует дров", book.ingredientsFor("campfire").containsKey("wood"));
        assertEquals("Костёр из 3 дров", 3, (int) book.ingredientsFor("campfire").get("wood"));
    }

    @Test
    public void crafting_tree_has_no_cycles() {
        CraftingBook book = new CraftingBook();
        assertFalse("Крафт-дерево не должно содержать циклов", book.hasCycles());
    }

    @Test
    public void crafting_produces_item() {
        CraftingBook book = new CraftingBook();
        var inv = new java.util.HashMap<String, Integer>();
        inv.put("wood", 5);
        assertTrue("Крафт удался", book.craft(inv, "campfire"));
        assertEquals("Дрова израсходованы", 2, (int) inv.get("wood"));
        assertEquals("Предмет создан", 1, (int) inv.get("campfire"));
    }

    @Test
    public void hunger_and_warmth_decay() {
        SurvivalState s = new SurvivalState();
        s.tick(false);
        assertTrue("Голод растёт", s.getHunger() > 0);
        assertTrue("Тепло падает", s.getWarmth() < 100);
    }

    @Test
    public void night_is_colder() {
        SurvivalState day = new SurvivalState();
        SurvivalState night = new SurvivalState();
        day.tick(true);
        night.tick(false);
        assertTrue("Ночью холоднее", night.getWarmth() < day.getWarmth());
    }

    @Test
    public void starving_damages_health() {
        SurvivalState s = new SurvivalState();
        s.setHunger(100);
        double hp = s.getHealth();
        s.tick(true);
        assertTrue("От голода падает здоровье", s.getHealth() < hp);
    }

    @Test
    public void eating_restores_hunger() {
        SurvivalState s = new SurvivalState();
        s.setHunger(80);
        s.eat(30);
        assertEquals("Еда восстанавливает голод", 50, (int) s.getHunger());
    }

    @Test
    public void score_grows_with_days() {
        assertTrue("Очки растут с днями", SurvivalState.scoreFor(5) > SurvivalState.scoreFor(2));
    }
}
