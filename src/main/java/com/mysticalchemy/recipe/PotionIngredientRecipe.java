package com.mysticalchemy.recipe;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.config.BrewingConfig;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;

public class PotionIngredientRecipe {
    private final ResourceLocation id;
    private final List<IngredientMatch> matches = new ArrayList<IngredientMatch>();
    private final HashMap<Potion, Float> potionEffects = new HashMap<Potion, Float>();
    private final EnumSet<Modifier> modifiers = EnumSet.noneOf(Modifier.class);
    private float potionAllEffectsDelta;
    private boolean potionCorruption;
    private boolean potionSplash;
    private boolean potionLingering;
    private int potionDuration;
    private PotionIngredientRecipe(ResourceLocation id) {
        this.id = id;
    }

    public static PotionIngredientRecipe fromConfigEntry(ResourceLocation id, String entry) {
        if (entry == null) {
            return null;
        }

        String trimmedEntry = stripCommentTags(entry).trim();
        if (trimmedEntry.isEmpty() || trimmedEntry.startsWith("#")) {
            return null;
        }

        PotionIngredientRecipe recipe = new PotionIngredientRecipe(id);
        String itemId = null;
        String oreDictId = null;
        Integer metadata = null;
        NBTTagCompound nbt = null;

        String[] tokens = trimmedEntry.split(";");
        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            int separator = trimmedToken.indexOf('=');
            if (separator <= 0 || separator == trimmedToken.length() - 1) {
                MysticAlchemy.LOGGER.warn("Skipping recipe {} with malformed token '{}'", id, trimmedToken);
                return null;
            }

            String key = trimmedToken.substring(0, separator).trim();
            String value = trimmedToken.substring(separator + 1).trim();
            if ("item".equals(key)) {
                itemId = value;
            } else if ("oredict".equals(key)) {
                oreDictId = value;
            } else if ("meta".equals(key) || "metadata".equals(key)) {
                Integer parsed = parseInteger(id, key, value);
                if (parsed == null) {
                    return null;
                }
                metadata = parsed;
            } else if ("nbt".equals(key)) {
                NBTTagCompound parsed = parseNbt(id, value);
                if (parsed == null) {
                    return null;
                }
                nbt = parsed;
            } else if ("potion_effects".equals(key)) {
                if (!parseEffects(recipe, id, value)) {
                    return null;
                }
            } else if ("potion_all_effects_delta".equals(key)) {
                Float parsed = parseFloat(id, "potion_all_effects_delta", value);
                if (parsed == null) {
                    return null;
                }
                recipe.potionAllEffectsDelta = parsed.floatValue();
            } else if ("potion_corruption".equals(key)) {
                Boolean parsed = parseBoolean(id, "potion_corruption", value);
                if (parsed == null) {
                    return null;
                }
                recipe.potionCorruption = parsed.booleanValue();
            } else if ("potion_duration".equals(key)) {
                Integer parsed = parseInteger(id, "potion_duration", value);
                if (parsed == null) {
                    return null;
                }
                recipe.potionDuration = parsed.intValue();
            } else if ("potion_splash".equals(key)) {
                Boolean parsed = parseBoolean(id, "potion_splash", value);
                if (parsed == null) {
                    return null;
                }
                recipe.potionSplash = parsed.booleanValue();
            } else if ("potion_lingering".equals(key)) {
                Boolean parsed = parseBoolean(id, "potion_lingering", value);
                if (parsed == null) {
                    return null;
                }
                recipe.potionLingering = parsed.booleanValue();
            } else if ("explode".equals(key)) {
                Boolean parsed = parseBoolean(id, "explode", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.EXPLODE, parsed.booleanValue());
            } else if ("evaporate_to_gas".equals(key)) {
                Boolean parsed = parseBoolean(id, "evaporate_to_gas", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.EVAPORATE_TO_GAS, parsed.booleanValue());
            } else if ("blinding_flash".equals(key)) {
                Boolean parsed = parseBoolean(id, "blinding_flash", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.BLINDING_FLASH, parsed.booleanValue());
            } else if ("verdant_growth".equals(key)) {
                Boolean parsed = parseBoolean(id, "verdant_growth", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.VERDANT_GROWTH, parsed.booleanValue());
            } else if ("ignition_burst".equals(key)) {
                Boolean parsed = parseBoolean(id, "ignition_burst", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.IGNITION_BURST, parsed.booleanValue());
            } else if ("spawn_magma_cube".equals(key)) {
                Boolean parsed = parseBoolean(id, "spawn_magma_cube", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.SPAWN_MAGMA_CUBE, parsed.booleanValue());
            } else if ("spawn_slime".equals(key)) {
                Boolean parsed = parseBoolean(id, "spawn_slime", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.SPAWN_SLIME, parsed.booleanValue());
            } else if ("jellify_potion".equals(key)) {
                Boolean parsed = parseBoolean(id, "jellify_potion", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.JELLYIFY_POTION, parsed.booleanValue());
            } else if ("potion_burst".equals(key)) {
                Boolean parsed = parseBoolean(id, "potion_burst", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.POTION_BURST, parsed.booleanValue());
            } else if ("potion_reverse".equals(key)) {
                Boolean parsed = parseBoolean(id, "potion_reverse", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.POTION_REVERSE, parsed.booleanValue());
            } else if ("turn_to_slime".equals(key)) {
                Boolean parsed = parseBoolean(id, "turn_to_slime", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.TURN_TO_SLIME, parsed.booleanValue());
            } else if ("stonification".equals(key)) {
                Boolean parsed = parseBoolean(id, "stonification", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.STONIFICATION, parsed.booleanValue());
            } else if ("melt_crucible".equals(key)) {
                Boolean parsed = parseBoolean(id, "melt_crucible", value);
                if (parsed == null) {
                    return null;
                }
                recipe.setModifier(Modifier.MELT_CRUCIBLE, parsed.booleanValue());
            } else {
                MysticAlchemy.LOGGER.warn("Skipping recipe {} with unsupported key '{}'", id, key);
                return null;
            }
        }

        if (itemId != null && oreDictId != null) {
            MysticAlchemy.LOGGER.warn("Skipping recipe {} because it defines both item and oredict", id);
            return null;
        }

        if (itemId != null) {
            recipe.matches.addAll(resolveItem(itemId));
        } else if (oreDictId != null) {
            recipe.matches.addAll(resolveOreDict(oreDictId));
        } else {
            MysticAlchemy.LOGGER.warn("Skipping recipe {} without item or oredict", id);
            return null;
        }

        if (recipe.matches.isEmpty()) {
            MysticAlchemy.LOGGER.warn("Skipping recipe {} because no matching ingredient exists", id);
            return null;
        }

        int stateConversionCount = 0;
        if (recipe.hasModifier(Modifier.TURN_TO_SLIME)) {
            stateConversionCount++;
        }
        if (recipe.hasModifier(Modifier.STONIFICATION)) {
            stateConversionCount++;
        }
        if (recipe.hasModifier(Modifier.MELT_CRUCIBLE)) {
            stateConversionCount++;
        }
        if (recipe.hasModifier(Modifier.JELLYIFY_POTION)) {
            stateConversionCount++;
        }
        if (stateConversionCount > 1) {
            MysticAlchemy.LOGGER.warn("Skipping recipe {} because it defines conflicting state conversions", id);
            return null;
        }

        if (metadata != null || nbt != null) {
            recipe.applyIngredientConstraints(metadata, nbt);
        }

        return recipe;
    }

    private static boolean parseEffects(PotionIngredientRecipe recipe, ResourceLocation id, String value) {
        if (value.isEmpty()) {
            return true;
        }

        String[] effectsArray = value.split(",");
        for (String effectEntry : effectsArray) {
            String trimmedEffect = effectEntry.trim();
            if (trimmedEffect.isEmpty()) {
                continue;
            }

            int separator = trimmedEffect.lastIndexOf('@');
            if (separator <= 0 || separator == trimmedEffect.length() - 1) {
                MysticAlchemy.LOGGER.warn("Skipping recipe {} with malformed effect '{}'", id, trimmedEffect);
                return false;
            }

            Potion potion = resolveEffect(trimmedEffect.substring(0, separator).trim());
            if (potion == null) {
                continue;
            }

            try {
                float strength = Float.parseFloat(trimmedEffect.substring(separator + 1).trim());
                recipe.potionEffects.put(potion, strength);
            } catch (NumberFormatException ex) {
                MysticAlchemy.LOGGER.warn("Skipping recipe {} with invalid strength '{}'", id, trimmedEffect, ex);
                return false;
            }
        }

        return true;
    }

    private static Integer parseInteger(ResourceLocation id, String key, String value) {
        try {
            return Integer.valueOf(Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            MysticAlchemy.LOGGER.warn("Skipping recipe {} with invalid integer {}={}", id, key, value, ex);
            return null;
        }
    }

    private static Float parseFloat(ResourceLocation id, String key, String value) {
        try {
            return Float.valueOf(Float.parseFloat(value));
        } catch (NumberFormatException ex) {
            MysticAlchemy.LOGGER.warn("Skipping recipe {} with invalid float {}={}", id, key, value, ex);
            return null;
        }
    }

    private static Boolean parseBoolean(ResourceLocation id, String key, String value) {
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }

        MysticAlchemy.LOGGER.warn("Skipping recipe {} with invalid boolean {}={}", id, key, value);
        return null;
    }

    private static NBTTagCompound parseNbt(ResourceLocation id, String value) {
        try {
            return JsonToNBT.getTagFromJson(value);
        } catch (NBTException ex) {
            MysticAlchemy.LOGGER.warn("Skipping recipe {} with invalid nbt={}", id, value, ex);
            return null;
        }
    }

    private static Potion resolveEffect(String effectId) {
        ResourceLocation effectLocation = new ResourceLocation(effectId);
        Potion potion = ForgeRegistries.POTIONS.getValue(effectLocation);
        if (potion == null) {
            MysticAlchemy.LOGGER.warn("Skipping unresolved potion effect {}", effectLocation);
        }
        return potion;
    }

    private static List<Potion> getPotionPool(boolean badEffects) {
        List<Potion> pool = new ArrayList<Potion>();
        for (Potion potion : Potion.REGISTRY) {
            if (potion == null || potion.getRegistryName() == null) {
                continue;
            }
            if (potion.isBadEffect() != badEffects) {
                continue;
            }
            if (!BrewingConfig.isEffectAllowedForRandomization(potion.getRegistryName())) {
                continue;
            }
            pool.add(potion);
        }
        return pool;
    }

    private static boolean isStateConversionModifier(Modifier modifier) {
        return modifier == Modifier.TURN_TO_SLIME
                || modifier == Modifier.STONIFICATION
                || modifier == Modifier.MELT_CRUCIBLE
                || modifier == Modifier.JELLYIFY_POTION;
    }

    private static List<IngredientMatch> resolveOreDict(String oreDictId) {
        List<IngredientMatch> matches = new ArrayList<IngredientMatch>();
        List<ItemStack> ores = OreDictionary.getOres(oreDictId, false);
        for (ItemStack stack : ores) {
            if (!stack.isEmpty()) {
                matches.add(new IngredientMatch(stack.getItem(), stack.getMetadata()));
            }
        }
        return matches;
    }

    private static String stripCommentTags(String entry) {
        StringBuilder builder = new StringBuilder(entry.length());
        boolean insideCommentTag = false;
        for (int i = 0; i < entry.length(); i++) {
            char ch = entry.charAt(i);
            if (ch == '/' && i + 1 < entry.length() && entry.charAt(i + 1) == '/') {
                insideCommentTag = !insideCommentTag;
                i++;
                continue;
            }
            if (!insideCommentTag) {
                builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static List<IngredientMatch> resolveItem(String itemId) {
        List<IngredientMatch> matches = new ArrayList<IngredientMatch>();
        Item item = Item.REGISTRY.getObject(new ResourceLocation(itemId));
        if (item != null) {
            matches.add(new IngredientMatch(item, OreDictionary.WILDCARD_VALUE));
        }
        return matches;
    }

    public boolean matches(ItemStack stack) {
        for (IngredientMatch match : matches) {
            if (match.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    public ResourceLocation getId() {
        return id;
    }

    public HashMap<Potion, Float> getPotionEffects() {
        return potionEffects;
    }

    @Deprecated
    public HashMap<Potion, Float> getEffects() {
        return getPotionEffects();
    }

    public float getPotionAllEffectsDelta() {
        return potionAllEffectsDelta;
    }

    @Deprecated
    public float getAllEffectsDelta() {
        return getPotionAllEffectsDelta();
    }

    public boolean isPotionCorruption() {
        return potionCorruption;
    }

    public HashMap<Potion, Float> getRandomizedEffects(long worldSeed) {
        List<Potion> goodPool = getPotionPool(false);
        List<Potion> badPool = getPotionPool(true);
        if (goodPool.isEmpty() && badPool.isEmpty()) {
            return new HashMap<Potion, Float>(potionEffects);
        }

        int targetCount = Math.max(2, potionEffects.size());
        Random random = new Random(combineSeed(worldSeed));
        List<Potion> selected = new ArrayList<Potion>(targetCount);
        boolean preferGood;

        if (!goodPool.isEmpty() && !badPool.isEmpty()) {
            selected.add(goodPool.get(random.nextInt(goodPool.size())));
            selected.add(badPool.get(random.nextInt(badPool.size())));
            preferGood = random.nextBoolean();
        } else {
            List<Potion> availablePool = !goodPool.isEmpty() ? goodPool : badPool;
            selected.add(availablePool.get(random.nextInt(availablePool.size())));
            preferGood = !goodPool.isEmpty();
        }

        int safety = 0;
        while (selected.size() < targetCount && safety < 128) {
            List<Potion> pool = preferGood ? goodPool : badPool;
            if (pool.isEmpty()) {
                pool = preferGood ? badPool : goodPool;
                if (pool.isEmpty()) {
                    break;
                }
            }
            Potion chosen = pool.get(random.nextInt(pool.size()));
            if (!selected.contains(chosen)) {
                selected.add(chosen);
            }
            preferGood = !preferGood;
            safety++;
        }

        HashMap<Potion, Float> randomized = new HashMap<Potion, Float>(selected.size());
        for (Potion potion : selected) {
            if (potion == null) {
                continue;
            }

            float strength;
            if (potion.isBadEffect()) {
                strength = 0.25f + random.nextFloat() * 0.3f;
            } else {
                strength = 0.35f + random.nextFloat() * 0.35f;
            }
            randomized.put(potion, strength);
        }
        return randomized;
    }

    public boolean isPotionSplash() {
        return potionSplash;
    }

    @Deprecated
    public boolean getMakesSplash() {
        return isPotionSplash();
    }

    public boolean isPotionLingering() {
        return potionLingering;
    }

    @Deprecated
    public boolean getMakesLingering() {
        return isPotionLingering();
    }

    public int getPotionDuration() {
        return potionDuration;
    }

    @Deprecated
    public int getDurationAdded() {
        return getPotionDuration();
    }

    public boolean isExplode() {
        return hasModifier(Modifier.EXPLODE);
    }

    public boolean isEvaporateToGas() {
        return hasModifier(Modifier.EVAPORATE_TO_GAS);
    }

    public boolean isBlindingFlash() {
        return hasModifier(Modifier.BLINDING_FLASH);
    }

    public boolean isVerdantGrowth() {
        return hasModifier(Modifier.VERDANT_GROWTH);
    }

    public boolean isIgnitionBurst() {
        return hasModifier(Modifier.IGNITION_BURST);
    }

    public boolean isSpawnMagmaCube() {
        return hasModifier(Modifier.SPAWN_MAGMA_CUBE);
    }

    public boolean isSpawnSlime() {
        return hasModifier(Modifier.SPAWN_SLIME);
    }

    public boolean isJellyifyPotion() {
        return hasModifier(Modifier.JELLYIFY_POTION);
    }

    public boolean isPotionBurst() {
        return hasModifier(Modifier.POTION_BURST);
    }

    public boolean isPotionReverse() {
        return hasModifier(Modifier.POTION_REVERSE);
    }

    public boolean isTurnToSlime() {
        return hasModifier(Modifier.TURN_TO_SLIME);
    }

    public boolean isStonification() {
        return hasModifier(Modifier.STONIFICATION);
    }

    public boolean isMeltCrucible() {
        return hasModifier(Modifier.MELT_CRUCIBLE);
    }

    public boolean hasModifier(Modifier modifier) {
        return modifiers.contains(modifier) && !BrewingConfig.isModifierDisabled(modifier);
    }

    public EnumSet<Modifier> getEnabledModifiers() {
        EnumSet<Modifier> enabled = EnumSet.noneOf(Modifier.class);
        for (Modifier modifier : modifiers) {
            if (!BrewingConfig.isModifierDisabled(modifier)) {
                enabled.add(modifier);
            }
        }
        return enabled;
    }

    public EnumSet<Modifier> getRandomizedModifiers(long worldSeed) {
        EnumSet<Modifier> enabled = getEnabledModifiers();
        int targetCount = enabled.size();
        return getRandomizedModifiers(worldSeed, targetCount);
    }

    public EnumSet<Modifier> getRandomizedModifiers(long worldSeed, int targetCount) {
        if (targetCount <= 0) {
            return EnumSet.noneOf(Modifier.class);
        }

        Random random = new Random(combineSeed(worldSeed) ^ 0xD6E8FEB86659FD93L);
        return selectRandomizedModifiers(random, targetCount);
    }

    public EnumSet<Modifier> getRandomizedModifiersForReplaceMode(long worldSeed) {
        Random random = new Random(combineSeed(worldSeed) ^ 0xD6E8FEB86659FD93L);
        int roll = random.nextInt(100);
        int targetCount;
        if (roll < 60) {
            targetCount = 0;
        } else if (roll < 90) {
            targetCount = 1;
        } else {
            targetCount = 2;
        }

        return selectRandomizedModifiers(random, targetCount);
    }

    private EnumSet<Modifier> selectRandomizedModifiers(Random random, int targetCount) {
        if (targetCount <= 0) {
            return EnumSet.noneOf(Modifier.class);
        }

        List<Modifier> pool = new ArrayList<Modifier>();
        for (Modifier modifier : Modifier.values()) {
            if (!BrewingConfig.isModifierDisabled(modifier)) {
                pool.add(modifier);
            }
        }
        if (pool.isEmpty()) {
            return EnumSet.noneOf(Modifier.class);
        }
        Collections.shuffle(pool, random);

        EnumSet<Modifier> randomized = EnumSet.noneOf(Modifier.class);
        boolean hasStateConversion = false;
        for (Modifier modifier : pool) {
            if (randomized.size() >= targetCount) {
                break;
            }
            if (isStateConversionModifier(modifier) && hasStateConversion) {
                continue;
            }
            randomized.add(modifier);
            if (isStateConversionModifier(modifier)) {
                hasStateConversion = true;
            }
        }
        return randomized;
    }

    public boolean hasAnyActionModifier() {
        for (Modifier modifier : modifiers) {
            if (hasModifier(modifier)) {
                return true;
            }
        }
        return false;
    }

    private void setModifier(Modifier modifier, boolean enabled) {
        if (enabled) {
            modifiers.add(modifier);
        } else {
            modifiers.remove(modifier);
        }
    }

    private void applyIngredientConstraints(Integer metadata, NBTTagCompound nbt) {
        List<IngredientMatch> constrained = new ArrayList<IngredientMatch>(matches.size());
        for (IngredientMatch match : matches) {
            int resolvedMetadata = metadata != null ? metadata.intValue() : match.metadata;
            constrained.add(new IngredientMatch(match.item, resolvedMetadata, nbt));
        }
        matches.clear();
        matches.addAll(constrained);
    }

    private long combineSeed(long worldSeed) {
        // 64-bit golden-ratio constant commonly used for seed/hash mixing.
        long mixed = worldSeed ^ 0x9E3779B97F4A7C15L;
        mixed ^= id.getNamespace().hashCode();
        // 31 is the standard Java-style hash multiplier.
        mixed ^= (long) id.getPath().hashCode() * 31L;
        // Additional odd mixing constant to decorrelate similar recipe shapes.
        mixed ^= (long) matches.size() * 1315423911L;
        return mixed;
    }

    public enum Modifier {
        EXPLODE,
        EVAPORATE_TO_GAS,
        BLINDING_FLASH,
        VERDANT_GROWTH,
        IGNITION_BURST,
        SPAWN_MAGMA_CUBE,
        SPAWN_SLIME,
        JELLYIFY_POTION,
        POTION_BURST,
        POTION_REVERSE,
        TURN_TO_SLIME,
        STONIFICATION,
        MELT_CRUCIBLE
    }

    public static class IngredientMatch {
        private final Item item;
        private final int metadata;
        private final NBTTagCompound requiredNbt;

        public IngredientMatch(Item item, int metadata) {
            this(item, metadata, null);
        }

        public IngredientMatch(Item item, int metadata, NBTTagCompound requiredNbt) {
            this.item = item;
            this.metadata = metadata;
            this.requiredNbt = requiredNbt;
        }

        public boolean matches(ItemStack stack) {
            return !stack.isEmpty()
                    && stack.getItem() == item
                    && (metadata == OreDictionary.WILDCARD_VALUE || stack.getMetadata() == metadata)
                    && NBTUtil.areNBTEquals(requiredNbt, stack.getTagCompound(), true);
        }
    }
}
