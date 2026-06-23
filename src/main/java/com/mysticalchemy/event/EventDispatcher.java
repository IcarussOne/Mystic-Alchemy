package com.mysticalchemy.event;

import com.mysticalchemy.api.events.CrucibleEvent;
import com.mysticalchemy.recipe.PotionIngredientRecipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.common.MinecraftForge;

import java.util.HashMap;

public final class EventDispatcher {
    private EventDispatcher() {
    }

    public static CrucibleEvent.AddIngredient dispatchCrucibleAddIngredientEvent(HashMap<Potion, Float> effects, PotionIngredientRecipe recipe, ItemStack stack) {
        CrucibleEvent.AddIngredient event = new CrucibleEvent.AddIngredient(effects, recipe, stack);
        MinecraftForge.EVENT_BUS.post(event);
        return event;
    }

    public static void dispatchCrucibleExtractPotionEvent(ItemStack potionStack, EntityPlayer player) {
        CrucibleEvent.ExtractPotion event = new CrucibleEvent.ExtractPotion(potionStack, player);
        MinecraftForge.EVENT_BUS.post(event);
    }
}
