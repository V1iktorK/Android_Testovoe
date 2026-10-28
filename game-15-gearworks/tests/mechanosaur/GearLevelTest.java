package mechanosaur;

import org.junit.Test;
import static org.junit.Assert.*;
import com.mechanosaur.game.LevelGenerator;
import com.mechanosaur.game.GearSystem;

/**
 * Тесты генератора уровней (ось C: параметрические шаблоны зубчатых передач).
 */
public class GearLevelTest {

    @Test
    public void generated_level_is_solvable() {
        for (long seed = 1; seed <= 12; seed++) {
            var level = LevelGenerator.generate(seed, 3);
            assertTrue("Эталонная сборка решает уровень (seed=" + seed + ")",
                    level.solvesReference());
        }
    }

    @Test
    public void generation_is_deterministic() {
        var a = LevelGenerator.generate(42, 4);
        var b = LevelGenerator.generate(42, 4);
        assertEquals("Детерминизм генератора", a.signature(), b.signature());
    }

    @Test
    public void difficulty_changes_ratio_target() {
        var easy = LevelGenerator.generate(7, 1);
        var hard = LevelGenerator.generate(7, 6);
        assertTrue("Сложность растёт с целевым числом",
                hard.targetRatio >= easy.targetRatio);
    }

    @Test
    public void inventory_matches_reference_solution() {
        var level = LevelGenerator.generate(5, 3);
        assertTrue("Инвентаря хватает на эталонную сборку", level.referenceFitsInventory());
    }

    @Test
    public void par_moves_positive() {
        for (long seed = 1; seed <= 10; seed++) {
            assertTrue("par > 0", LevelGenerator.generate(seed, 2).parMoves() > 0);
        }
    }

    @Test
    public void reference_achieves_target_ratio() {
        var level = LevelGenerator.generate(11, 5);
        GearSystem ref = level.buildReference();
        double rpm = ref.outputRpm(0, level.gearCount() - 1, level.inputRpm);
        assertEquals("Эталон даёт целевое передаточное число", level.targetRatio,
                rpm / level.inputRpm, 0.01);
    }
}
