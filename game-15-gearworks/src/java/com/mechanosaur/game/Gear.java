package com.mechanosaur.game;

/** Шестерня: центр в клетках и радиус (в клетках). */
public final class Gear {
    public final int x;
    public final int y;
    public final int radius;

    public Gear(int x, int y, int radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    /** Шестерни сцеплены, если расстояние между центрами равно сумме радиусов. */
    public boolean meshesWith(Gear other) {
        long dx = x - other.x;
        long dy = y - other.y;
        long r = radius + other.radius;
        return dx * dx + dy * dy == r * r;
    }

    public boolean overlaps(Gear other) {
        long dx = x - other.x;
        long dy = y - other.y;
        long rr = Math.max(radius, other.radius);
        return dx * dx + dy * dy < rr * rr;
    }
}
