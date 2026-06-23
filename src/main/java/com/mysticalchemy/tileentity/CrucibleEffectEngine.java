package com.mysticalchemy.tileentity;

import com.mysticalchemy.config.BrewingConfig;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.*;

public final class CrucibleEffectEngine {
    private static final float INSTABILITY_STEP = 0.2f;
    private static final HashMap<ResourceLocation, ResourceLocation> REVERSE_POTION_MAP = new HashMap<ResourceLocation, ResourceLocation>();

    static {
        addReversePair("minecraft:speed", "minecraft:slowness");
        addReversePair("minecraft:strength", "minecraft:weakness");
        addReversePair("minecraft:haste", "minecraft:mining_fatigue");
        addReversePair("minecraft:instant_health", "minecraft:instant_damage");
        addReversePair("minecraft:regeneration", "minecraft:poison");
        addReversePair("minecraft:night_vision", "minecraft:blindness");
        addReversePair("minecraft:jump_boost", "minecraft:levitation");
        addReversePair("minecraft:luck", "minecraft:unluck");
    }

    private CrucibleEffectEngine() {
    }

    private static void addReversePair(String a, String b) {
        ResourceLocation left = new ResourceLocation(a);
        ResourceLocation right = new ResourceLocation(b);
        REVERSE_POTION_MAP.put(left, right);
        REVERSE_POTION_MAP.put(right, left);
    }

    public static void applyAllEffectsDelta(HashMap<Potion, Float> effectStrengths, float delta, int quantity) {
        float totalDelta = delta * quantity;
        if (totalDelta == 0.0f || effectStrengths.isEmpty()) {
            return;
        }

        List<Potion> currentPotions = new ArrayList<Potion>(effectStrengths.keySet());
        for (Potion potion : currentPotions) {
            if (potion == null || BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                continue;
            }

            float current = effectStrengths.get(potion).floatValue();
            float updated = current + totalDelta;
            if (updated <= 0.0f) {
                effectStrengths.remove(potion);
            } else {
                effectStrengths.put(potion, updated);
            }
        }
    }

    public static void mergeEffects(HashMap<Potion, Float> effectStrengths, HashMap<Potion, Float> effects, int quantity) {
        for (Map.Entry<Potion, Float> entry : effects.entrySet()) {
            Potion potion = entry.getKey();
            if (potion == null || BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                continue;
            }

            float current = effectStrengths.containsKey(potion) ? effectStrengths.get(potion).floatValue() : 0.0f;
            float updatedMagnitude = current + entry.getValue().floatValue() * quantity;
            if (updatedMagnitude <= 0.0f) {
                effectStrengths.remove(potion);
            } else {
                effectStrengths.put(potion, updatedMagnitude);
            }
        }
    }

    public static HashMap<Potion, Float> getReversedEffects(HashMap<Potion, Float> source) {
        HashMap<Potion, Float> reversed = new HashMap<Potion, Float>();
        for (Map.Entry<Potion, Float> entry : source.entrySet()) {
            Potion potion = entry.getKey();
            if (potion == null || potion.getRegistryName() == null) {
                continue;
            }

            ResourceLocation targetId = REVERSE_POTION_MAP.get(potion.getRegistryName());
            Potion mappedPotion = targetId == null ? null : ForgeRegistries.POTIONS.getValue(targetId);
            Potion finalPotion = mappedPotion != null ? mappedPotion : potion;
            float current = reversed.containsKey(finalPotion) ? reversed.get(finalPotion).floatValue() : 0.0f;
            reversed.put(finalPotion, current + entry.getValue().floatValue());
        }
        return reversed;
    }

    public static boolean canMerge(HashMap<Potion, Float> currentEffects, boolean applyReverse, float allEffectsDelta, HashMap<Potion, Float> recipeEffects, int quantity, int maxProminentEffects, int currentDuration, int durationAdded, int maxDuration) {
        HashMap<Potion, Float> simulated = new HashMap<Potion, Float>(currentEffects);
        if (applyReverse) {
            simulated = getReversedEffects(simulated);
        }

        if (allEffectsDelta != 0.0f) {
            List<Potion> currentPotions = new ArrayList<Potion>(simulated.keySet());
            for (Potion potion : currentPotions) {
                if (potion == null || BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                    continue;
                }

                float currentMagnitude = simulated.get(potion).floatValue();
                float newMagnitude = Math.max(0.0f, currentMagnitude + allEffectsDelta * quantity);
                float maxMagnitude = BrewingConfig.getAmplifierCap(potion.getRegistryName()) + 1.0f;
                if (newMagnitude > maxMagnitude) {
                    return false;
                }

                if (newMagnitude <= 0.0f) {
                    simulated.remove(potion);
                } else {
                    simulated.put(potion, newMagnitude);
                }
            }
        }

        for (Map.Entry<Potion, Float> entry : recipeEffects.entrySet()) {
            Potion potion = entry.getKey();
            if (potion == null || BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                continue;
            }

            float addedMagnitude = entry.getValue().floatValue() * quantity;
            float currentMagnitude = simulated.containsKey(potion) ? simulated.get(potion).floatValue() : 0.0f;
            float newMagnitude = Math.max(0.0f, currentMagnitude + addedMagnitude);
            float maxMagnitude = BrewingConfig.getAmplifierCap(potion.getRegistryName()) + 1.0f;
            if (newMagnitude > maxMagnitude) {
                return false;
            }

            if (newMagnitude <= 0.0f) {
                simulated.remove(potion);
            } else {
                simulated.put(potion, newMagnitude);
            }
        }

        int amplifiedCount = 0;
        for (Map.Entry<Potion, Float> entry : simulated.entrySet()) {
            Potion potion = entry.getKey();
            if (potion != null && entry.getValue().floatValue() > 1.0f && !BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                amplifiedCount++;
            }
        }
        if (amplifiedCount > maxProminentEffects) {
            return false;
        }

        if (durationAdded > 0 && currentDuration + durationAdded * quantity > maxDuration) {
            return false;
        }

        return true;
    }

    public static InstabilityState tickInstability(HashMap<Potion, Float> effectStrengths, boolean instabilityActive, float heat, float boilPoint, int updateRate, int instabilityTicks, int intervalTicks, boolean nextBad, int goodIndex, int badIndex) {
        if (!instabilityActive || heat < boilPoint || effectStrengths.isEmpty()) {
            return new InstabilityState(0, nextBad, goodIndex, badIndex, false);
        }

        int nextTicks = instabilityTicks + updateRate;
        if (nextTicks < intervalTicks) {
            return new InstabilityState(nextTicks, nextBad, goodIndex, badIndex, false);
        }

        boolean badEffect = nextBad;
        InstabilityMutationResult mutation = mutateInstabilityEffect(effectStrengths, badEffect, goodIndex, badIndex);
        return new InstabilityState(0, !nextBad, mutation.goodIndex, mutation.badIndex, mutation.changed);
    }

    private static InstabilityMutationResult mutateInstabilityEffect(HashMap<Potion, Float> effectStrengths, boolean badEffect, int goodIndex, int badIndex) {
        List<Potion> candidates = new ArrayList<Potion>();
        for (Potion potion : effectStrengths.keySet()) {
            if (potion == null || potion.getRegistryName() == null || potion.isBadEffect() != badEffect || BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                continue;
            }
            candidates.add(potion);
        }

        if (candidates.isEmpty()) {
            return new InstabilityMutationResult(false, goodIndex, badIndex);
        }

        Collections.sort(candidates, (left, right) -> left.getRegistryName().toString().compareTo(right.getRegistryName().toString()));
        int startIndex = badEffect ? badIndex : goodIndex;
        int size = candidates.size();
        for (int offset = 0; offset < size; offset++) {
            int index = (startIndex + offset) % size;
            Potion potion = candidates.get(index);
            float current = effectStrengths.get(potion).floatValue();
            float maxMagnitude = BrewingConfig.getAmplifierCap(potion.getRegistryName()) + 1.0f;
            float updated = badEffect ? Math.min(maxMagnitude, current + INSTABILITY_STEP) : Math.max(0.0f, current - INSTABILITY_STEP);
            if (updated == current) {
                continue;
            }

            if (updated <= 0.0f) {
                effectStrengths.remove(potion);
            } else {
                effectStrengths.put(potion, updated);
            }
            if (badEffect) {
                return new InstabilityMutationResult(true, goodIndex, (index + 1) % size);
            }
            return new InstabilityMutationResult(true, (index + 1) % size, badIndex);
        }

        return new InstabilityMutationResult(false, goodIndex, badIndex);
    }

    public static final class InstabilityState {
        public final int instabilityTicks;
        public final boolean instabilityNextBadEffect;
        public final int instabilityGoodIndex;
        public final int instabilityBadIndex;
        public final boolean changed;

        public InstabilityState(int instabilityTicks, boolean instabilityNextBadEffect, int instabilityGoodIndex, int instabilityBadIndex, boolean changed) {
            this.instabilityTicks = instabilityTicks;
            this.instabilityNextBadEffect = instabilityNextBadEffect;
            this.instabilityGoodIndex = instabilityGoodIndex;
            this.instabilityBadIndex = instabilityBadIndex;
            this.changed = changed;
        }
    }

    private static final class InstabilityMutationResult {
        private final boolean changed;
        private final int goodIndex;
        private final int badIndex;

        private InstabilityMutationResult(boolean changed, int goodIndex, int badIndex) {
            this.changed = changed;
            this.goodIndex = goodIndex;
            this.badIndex = badIndex;
        }
    }
}
