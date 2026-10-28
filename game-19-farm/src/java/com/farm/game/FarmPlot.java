package com.farm.game;

/** Грядка: рост культуры, полив, сбор урожая. */
public final class FarmPlot {

    private final Crop crop;
    private double growth;
    private boolean watered;
    private boolean harvested;

    public FarmPlot(Crop crop) {
        this.crop = crop;
    }

    public Crop crop() {
        return crop;
    }

    public double growth() {
        return growth;
    }

    /** Полив ускоряет рост в 1.5 раза. */
    public void water() {
        watered = true;
    }

    public void tick(double seconds) {
        if (isRipe()) return;
        double speed = watered ? 1.5 : 1.0;
        growth += seconds * speed / crop.growSeconds;
        if (growth > 1.0) growth = 1.0;
    }

    public boolean isRipe() {
        return growth >= 1.0;
    }

    /** Сбор урожая: один раз за посев. */
    public int harvest() {
        if (!isRipe() || harvested) return 0;
        harvested = true;
        return crop.yieldAmount;
    }
}
