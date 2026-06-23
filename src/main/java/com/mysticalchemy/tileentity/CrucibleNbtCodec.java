package com.mysticalchemy.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public final class CrucibleNbtCodec {
    private static final String KEY_SPLASH = "Splash";
    private static final String KEY_LINGERING = "Lingering";
    private static final String KEY_DURATION = "Duration";
    private static final String KEY_TARGET_COLOR = "TargetColor";
    private static final String KEY_START_COLOR = "StartColor";
    private static final String KEY_INFUSE_TICKS = "InfuseTicks";
    private static final String KEY_EFFECTS = "Effects";
    private static final String KEY_INGREDIENTS = "Ingredients";

    private CrucibleNbtCodec() {
    }

    public static NBTTagCompound writeEffects(HashMap<Potion, Float> effects) {
        NBTTagCompound tag = new NBTTagCompound();
        for (Map.Entry<Potion, Float> entry : effects.entrySet()) {
            if (entry.getKey() != null && entry.getKey().getRegistryName() != null) {
                tag.setFloat(entry.getKey().getRegistryName().toString(), entry.getValue().floatValue());
            }
        }
        return tag;
    }

    public static HashMap<Potion, Float> readEffects(NBTTagCompound tag) {
        HashMap<Potion, Float> effects = new HashMap<Potion, Float>();
        for (String key : tag.getKeySet()) {
            Potion potion = ForgeRegistries.POTIONS.getValue(new ResourceLocation(key));
            if (potion != null) {
                effects.put(potion, tag.getFloat(key));
            }
        }
        return effects;
    }

    public static NBTTagList writeIngredients(HashMap<String, Integer> ingredientCounts) {
        NBTTagList ingredientsTag = new NBTTagList();
        for (Map.Entry<String, Integer> entry : ingredientCounts.entrySet()) {
            if (entry.getValue().intValue() <= 0) {
                continue;
            }

            NBTTagCompound ingredientTag = new NBTTagCompound();
            ingredientTag.setString("Key", entry.getKey());
            ingredientTag.setInteger("Count", entry.getValue().intValue());
            ingredientsTag.appendTag(ingredientTag);
        }
        return ingredientsTag;
    }

    public static HashMap<String, Integer> readIngredients(NBTTagList ingredientsTag) {
        HashMap<String, Integer> ingredientCounts = new HashMap<String, Integer>();
        for (int index = 0; index < ingredientsTag.tagCount(); index++) {
            NBTTagCompound ingredientTag = ingredientsTag.getCompoundTagAt(index);
            String key = ingredientTag.getString("Key");
            int count = ingredientTag.getInteger("Count");
            if (!key.isEmpty() && count > 0) {
                ingredientCounts.put(key, count);
            }
        }
        return ingredientCounts;
    }

    public static void writeBrewState(CrucibleBrewState brewState, NBTTagCompound compound) {
        compound.setBoolean(KEY_SPLASH, brewState.splash);
        compound.setBoolean(KEY_LINGERING, brewState.lingering);
        compound.setInteger(KEY_DURATION, brewState.duration);
        compound.setInteger(KEY_TARGET_COLOR, brewState.targetColor);
        compound.setInteger(KEY_START_COLOR, brewState.startColor);
        compound.setInteger(KEY_INFUSE_TICKS, brewState.infuseTicks);
        compound.setTag(KEY_EFFECTS, writeEffects(brewState.effectStrengths));
        compound.setTag(KEY_INGREDIENTS, writeIngredients(brewState.ingredientCounts));
    }

    public static void readBrewState(CrucibleBrewState brewState, NBTTagCompound compound, int defaultDuration, int defaultColor, int infusionTicks) {
        brewState.reset(defaultDuration, defaultColor, infusionTicks);
        brewState.splash = compound.getBoolean(KEY_SPLASH);
        brewState.lingering = compound.getBoolean(KEY_LINGERING);
        brewState.duration = compound.hasKey(KEY_DURATION) ? compound.getInteger(KEY_DURATION) : defaultDuration;
        brewState.targetColor = compound.hasKey(KEY_TARGET_COLOR) ? compound.getInteger(KEY_TARGET_COLOR) : defaultColor;
        brewState.startColor = compound.hasKey(KEY_START_COLOR) ? compound.getInteger(KEY_START_COLOR) : brewState.targetColor;
        brewState.infuseTicks = compound.hasKey(KEY_INFUSE_TICKS) ? compound.getInteger(KEY_INFUSE_TICKS) : infusionTicks;
        brewState.effectStrengths.putAll(readEffects(compound.getCompoundTag(KEY_EFFECTS)));
        if (compound.hasKey(KEY_INGREDIENTS, 9)) {
            brewState.ingredientCounts.putAll(readIngredients(compound.getTagList(KEY_INGREDIENTS, 10)));
        }
    }
}
