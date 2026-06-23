package com.mysticalchemy.registry;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.config.Config;
import com.mysticalchemy.recipe.PotionIngredientRecipe;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public final class IngredientLoader {
    private static final List<PotionIngredientRecipe> POTION_RECIPES = new ArrayList<>();

    private IngredientLoader() {
    }

    public static void loadPotionIngredientRecipes() {
        reloadPotionIngredientRecipes();
    }

    public static void reloadPotionIngredientRecipes() {
        POTION_RECIPES.clear();
        String[] definitions = Config.ingredientList != null ? Config.ingredientList.ingredient_definitions : new String[0];
        for (int index = 0; index < definitions.length; index++) {
            PotionIngredientRecipe recipe = PotionIngredientRecipe.fromConfigEntry(createConfigRecipeId(definitions[index], index), definitions[index]);
            if (recipe != null) {
                POTION_RECIPES.add(recipe);
            }
        }
        MysticAlchemy.LOGGER.debug("Loaded {} potion ingredient recipes from config", POTION_RECIPES.size());
    }

    public static PotionIngredientRecipe findMatchingRecipe(ItemStack stack) {
        for (PotionIngredientRecipe recipe : POTION_RECIPES) {
            if (recipe.matches(stack)) {
                return recipe;
            }
        }
        return null;
    }

    public static boolean isRandomizedPropertyEnabled(PotionIngredientRecipe recipe, long worldSeed, int coveragePercent) {
        if (recipe == null || coveragePercent <= 0) {
            return false;
        }
        if (coveragePercent >= 100) {
            return true;
        }
        if (POTION_RECIPES.isEmpty()) {
            return false;
        }

        int activeCount = Math.round(POTION_RECIPES.size() * (coveragePercent / 100.0f));
        if (activeCount <= 0) {
            return false;
        }
        if (activeCount >= POTION_RECIPES.size()) {
            return true;
        }

        List<ResourceLocation> shuffled = new ArrayList<ResourceLocation>(POTION_RECIPES.size());
        for (PotionIngredientRecipe candidate : POTION_RECIPES) {
            shuffled.add(candidate.getId());
        }
        Collections.shuffle(shuffled, new Random(worldSeed ^ 0xB0F2B4D36D1B57A9L));

        Set<ResourceLocation> enabled = new HashSet<ResourceLocation>(activeCount);
        for (int i = 0; i < activeCount; i++) {
            enabled.add(shuffled.get(i));
        }
        return enabled.contains(recipe.getId());
    }

    private static ResourceLocation createConfigRecipeId(String entry, int index) {
        String path = "entry_" + index;
        String kind = "line";
        if (entry != null) {
            String[] tokens = stripCommentTags(entry).split(";");
            for (String token : tokens) {
                String trimmedToken = token.trim();
                if (trimmedToken.startsWith("item=")) {
                    kind = "item";
                    path = sanitizePath(trimmedToken.substring("item=".length()));
                    break;
                }
                if (trimmedToken.startsWith("oredict=")) {
                    kind = "oredict";
                    path = sanitizePath(trimmedToken.substring("oredict=".length()));
                    break;
                }
            }
        }

        return new ResourceLocation(MysticAlchemy.MODID, "config/" + kind + "/" + path + "_" + index);
    }

    private static String sanitizePath(String value) {
        StringBuilder builder = new StringBuilder();
        String lowercase = value.toLowerCase(Locale.ROOT);
        for (int i = 0; i < lowercase.length(); i++) {
            char ch = lowercase.charAt(i);
            if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9') || ch == '_' || ch == '-' || ch == '/') {
                builder.append(ch);
            } else if (ch == ':') {
                builder.append('/');
            } else {
                builder.append('_');
            }
        }
        return builder.toString();
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
}
