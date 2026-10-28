package com.neonidle.game;

/** Описание генератора энергии: кривая стоимости (геометрическая) и производство. */
public final class GeneratorDef {
    public final String name;
    public final double baseCost;
    public final double costGrowth;
    public final double baseProduction;

    public GeneratorDef(String name, double baseCost, double costGrowth, double baseProduction) {
        this.name = name;
        this.baseCost = baseCost;
        this.costGrowth = costGrowth;
        this.baseProduction = baseProduction;
    }

    /** Стоимость покупки level-го генератора (0-й уровень — первый). */
    public double costFor(int level) {
        return baseCost * Math.pow(costGrowth, level);
    }

    /** Производство одного генератора в секунду. */
    public double production() {
        return baseProduction;
    }
}
