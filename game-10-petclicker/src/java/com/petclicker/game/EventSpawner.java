package com.petclicker.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Ивент-спавнер кликера (ось C: цепочки ивентов с детерминированной генерацией).
 * Золотая лапка, радужный мячик, сон-ивент и др. приходят по расписанию из сида.
 */
public final class EventSpawner {

    public enum Type { GOLDEN_PAW, RAINBOW_BALL, NAP_TIME, TREAT_SHOWER }

    public static final class GameEvent {
        public final double timeSeconds;
        public final Type type;

        GameEvent(double timeSeconds, Type type) {
            this.timeSeconds = timeSeconds;
            this.type = type;
        }
    }

    private EventSpawner() {
    }

    /** Детерминированная серия из count ивентов: времена и типы по сиду. */
    public static List<GameEvent> generate(long seed, int count) {
        Random rng = new Random(seed);
        List<GameEvent> events = new ArrayList<>();
        double time = 5.0;
        for (int i = 0; i < count; i++) {
            time += 8.0 + rng.nextDouble() * 14.0;
            Type type;
            double roll = rng.nextDouble();
            if (roll < 0.25) type = Type.GOLDEN_PAW;
            else if (roll < 0.5) type = Type.RAINBOW_BALL;
            else if (roll < 0.75) type = Type.NAP_TIME;
            else type = Type.TREAT_SHOWER;
            events.add(new GameEvent(time, type));
        }
        // Гарантия: хотя бы одна золотая лапка в серии
        if (count > 0) {
            boolean anyGolden = false;
            for (GameEvent e : events) if (e.type == Type.GOLDEN_PAW) anyGolden = true;
            if (!anyGolden) {
                GameEvent first = events.get(0);
                events.set(0, new GameEvent(first.timeSeconds, Type.GOLDEN_PAW));
            }
        }
        return events;
    }
}
