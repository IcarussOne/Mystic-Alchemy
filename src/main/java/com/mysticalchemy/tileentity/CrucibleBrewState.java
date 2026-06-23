package com.mysticalchemy.tileentity;

import net.minecraft.potion.Potion;

import java.util.HashMap;

public final class CrucibleBrewState {
    public final HashMap<Potion, Float> effectStrengths = new HashMap<Potion, Float>();
    public final HashMap<String, Integer> ingredientCounts = new HashMap<String, Integer>();
    public boolean splash;
    public boolean lingering;
    public int duration;
    public int targetColor;
    public int startColor;
    public int infuseTicks;

    public void reset(int defaultDuration, int defaultColor, int infusionTicks) {
        splash = false;
        lingering = false;
        duration = defaultDuration;
        targetColor = defaultColor;
        startColor = defaultColor;
        infuseTicks = infusionTicks;
        effectStrengths.clear();
        ingredientCounts.clear();
    }
}
