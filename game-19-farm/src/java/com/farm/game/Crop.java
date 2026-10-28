package com.farm.game;

/** Культура: время роста, урожай, цена и редкость. */
public final class Crop {
    public final String name;
    public final int growSeconds;
    public final int yieldAmount;
    public final int sellPrice;
    public final int rarity;

    Crop(String name, int growSeconds, int yieldAmount, int sellPrice, int rarity) {
        this.name = name;
        this.growSeconds = growSeconds;
        this.yieldAmount = yieldAmount;
        this.sellPrice = sellPrice;
        this.rarity = rarity;
    }
}
