package com.mysticalchemy.config;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.recipe.PotionIngredientRecipe;
import net.minecraft.util.ResourceLocation;

public final class BrewingConfig {
    private static boolean randomizationFilterConflictLogged;
    private static boolean invalidRandomizationModeLogged;
    private static boolean invalidForcedSeedLogged;
    private BrewingConfig() {
    }

    public static boolean isEffectDisabled(ResourceLocation effectId) {
        if (effectId == null) {
            return false;
        }

        String effectKey = effectId.toString();
        for (String entry : Config.brewing.ma_disallowed_effects) {
            if (effectKey.equals(entry.trim())) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEffectAllowedForRandomization(ResourceLocation effectId) {
        if (effectId == null) {
            return false;
        }

        String effectKey = effectId.toString();
        String[] whitelist = Config.randomization != null ? Config.randomization.randomization_effect_whitelist : new String[0];
        String[] blacklist = Config.brewing != null ? Config.brewing.ma_disallowed_effects : new String[0];
        boolean hasWhitelist = hasConfiguredEntries(whitelist);
        boolean hasBlacklist = hasConfiguredEntries(blacklist);

        if (hasWhitelist && hasBlacklist && !randomizationFilterConflictLogged) {
            randomizationFilterConflictLogged = true;
            MysticAlchemy.LOGGER.warn("Both randomization_effect_whitelist and ma_disallowed_effects are configured. Randomization will use the whitelist only.");
        }

        if (hasWhitelist) {
            return containsEffectId(whitelist, effectKey);
        }
        if (hasBlacklist) {
            return !containsEffectId(blacklist, effectKey);
        }
        return true;
    }

    public static RandomizationMode getRandomizationMode() {
        if (Config.randomization == null) {
            return RandomizationMode.OFF;
        }

        String configured = Config.randomization.randomization_mode;
        if (configured == null) {
            return RandomizationMode.REPLACE;
        }

        String normalized = configured.trim().toLowerCase();
        switch (normalized) {
            case "off":
                return RandomizationMode.OFF;
            case "merge":
                return RandomizationMode.MERGE;
            case "replace":
            case "":
                return RandomizationMode.REPLACE;
            default:
                if (!invalidRandomizationModeLogged) {
                    invalidRandomizationModeLogged = true;
                    MysticAlchemy.LOGGER.warn("Invalid randomization_mode='{}'. Falling back to 'replace'.", configured);
                }
                return RandomizationMode.REPLACE;
        }
    }

    public static long getRandomizationSeed(long worldSeed) {
        if (Config.randomization == null) {
            return worldSeed;
        }

        String configured = Config.randomization.forced_randomization_seed;
        if (configured == null) {
            return worldSeed;
        }

        String trimmed = configured.trim();
        if (trimmed.isEmpty()) {
            return worldSeed;
        }

        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ex) {
            if (!invalidForcedSeedLogged) {
                invalidForcedSeedLogged = true;
                MysticAlchemy.LOGGER.warn("Invalid forced_randomization_seed='{}'. Falling back to world seed.", configured);
            }
            return worldSeed;
        }
    }

    public static int getGlobalAmplifierCap() {
        Config.Brewing brewing = Config.brewing;
        if (brewing == null) {
            return 4;
        }
        return Math.max(0, brewing.ma_global_amplifier_cap);
    }

    public static int getAmplifierCap(ResourceLocation effectId) {
        int globalCap = getGlobalAmplifierCap();
        if (effectId == null || Config.brewing == null) {
            return globalCap;
        }

        String effectKey = effectId.toString();
        for (String entry : Config.brewing.ma_per_effect_amplifier_caps) {
            if (entry == null) {
                continue;
            }

            String trimmed = entry.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int separator = trimmed.indexOf('=');
            if (separator <= 0 || separator == trimmed.length() - 1) {
                continue;
            }

            String key = trimmed.substring(0, separator).trim();
            if (!effectKey.equals(key)) {
                continue;
            }

            String value = trimmed.substring(separator + 1).trim();
            try {
                return Math.max(0, Integer.parseInt(value));
            } catch (NumberFormatException ignored) {
                return globalCap;
            }
        }

        return globalCap;
    }

    public static int getMaxEffectsAboveOne() {
        Config.Brewing brewing = Config.brewing;
        if (brewing == null) {
            return 4;
        }
        return Math.max(0, brewing.ma_max_effects_above_one);
    }

    public static int getRandomizedPropertyCoveragePercent() {
        if (Config.randomization == null) {
            return 100;
        }
        return Math.max(0, Math.min(100, Config.randomization.randomized_property_coverage_percent));
    }

    public static float getBlindingFlashRadius() {
        if (Config.modifierTweaks == null) {
            return 3.0f;
        }
        return (float) Math.max(0.0d, Config.modifierTweaks.ma_blinding_flash_radius);
    }

    public static int getBlindingFlashDurationTicks() {
        if (Config.modifierTweaks == null) {
            return 100;
        }
        return Math.max(1, Config.modifierTweaks.ma_blinding_flash_duration_ticks);
    }

    public static float getIgnitionBurstRadius() {
        if (Config.modifierTweaks == null) {
            return 3.0f;
        }
        return (float) Math.max(0.0d, Config.modifierTweaks.ma_ignition_burst_radius);
    }

    public static int getIgnitionBurstFireSeconds() {
        if (Config.modifierTweaks == null) {
            return 5;
        }
        return Math.max(1, Config.modifierTweaks.ma_ignition_burst_fire_seconds);
    }

    public static float getPotionSlimeJellyDropChance() {
        if (Config.modifierTweaks == null) {
            return 0.2f;
        }
        return (float) Math.max(0.0d, Math.min(1.0d, Config.modifierTweaks.ma_potion_slime_jelly_drop_chance));
    }

    public static boolean isModifierDisabled(PotionIngredientRecipe.Modifier modifier) {
        if (modifier == null || Config.modifierTweaks == null) {
            return false;
        }

        String key = getModifierKey(modifier);
        if (key.isEmpty()) {
            return false;
        }

        for (String entry : Config.modifierTweaks.ma_disabled_modifiers) {
            if (entry != null && key.equals(entry.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasConfiguredEntries(String[] entries) {
        if (entries == null) {
            return false;
        }
        for (String entry : entries) {
            if (entry != null && !entry.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsEffectId(String[] entries, String effectKey) {
        if (entries == null || effectKey == null) {
            return false;
        }
        for (String entry : entries) {
            if (effectKey.equals(entry != null ? entry.trim() : "")) {
                return true;
            }
        }
        return false;
    }

    private static String getModifierKey(PotionIngredientRecipe.Modifier modifier) {
        switch (modifier) {
            case EXPLODE:
                return "explode";
            case EVAPORATE_TO_GAS:
                return "evaporate_to_gas";
            case BLINDING_FLASH:
                return "blinding_flash";
            case VERDANT_GROWTH:
                return "verdant_growth";
            case IGNITION_BURST:
                return "ignition_burst";
            case SPAWN_MAGMA_CUBE:
                return "spawn_magma_cube";
            case SPAWN_SLIME:
                return "spawn_slime";
            case JELLYIFY_POTION:
                return "jellify_potion";
            case POTION_BURST:
                return "potion_burst";
            case POTION_REVERSE:
                return "potion_reverse";
            case TURN_TO_SLIME:
                return "turn_to_slime";
            case STONIFICATION:
                return "stonification";
            case MELT_CRUCIBLE:
                return "melt_crucible";
            default:
                return "";
        }
    }

    public enum RandomizationMode {
        OFF,
        MERGE,
        REPLACE
    }
}
