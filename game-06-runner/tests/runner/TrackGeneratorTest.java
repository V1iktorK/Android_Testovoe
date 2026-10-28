package runner;

import org.junit.Test;
import static org.junit.Assert.*;
import com.runner.game.TrackGenerator;
import com.runner.game.LaneState;
import com.runner.game.RunRules;

/**
 * Тесты генератора трассы раннера (ось C: чанковая генерация — грамматика чанков).
 */
public class TrackGeneratorTest {

    @Test
    public void generate_isDeterministic() {
        String a = TrackGenerator.generate(42, 30);
        String b = TrackGenerator.generate(42, 30);
        assertEquals("Генератор должен быть детерминированным", a, b);
    }

    @Test
    public void generate_differentSeeds_differ() {
        String a = TrackGenerator.generate(1, 30);
        String b = TrackGenerator.generate(2, 30);
        assertNotEquals("Разные сиды дают разные трассы", a, b);
    }

    @Test
    public void generate_endsWithSafeRun() {
        for (long seed = 1; seed <= 10; seed++) {
            String track = TrackGenerator.generate(seed, 40);
            assertTrue("Трасса должна заканчиваться свободным отрезком",
                    track.endsWith("......"));
        }
    }

    @Test
    public void chunks_areFromGrammar() {
        String track = TrackGenerator.generate(7, 60);
        for (char c : track.toCharArray()) {
            assertTrue("Недопустимый символ трассы: " + c,
                    "xhulb.".indexOf(c) >= 0);
        }
    }

    @Test
    public void noTripleWalls() {
        // Три подряд высоких препятствия делают полосу непроходимой
        for (long seed = 1; seed <= 25; seed++) {
            String track = TrackGenerator.generate(seed, 80);
            assertFalse("Три высоких препятствия подряд (seed=" + seed + ")",
                    track.contains("xxx"));
        }
    }

    @Test
    public void trackLengthMatchesRequest() {
        assertEquals(50, TrackGenerator.generate(3, 50).length());
    }
}
