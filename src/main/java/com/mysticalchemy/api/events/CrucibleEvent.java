package com.mysticalchemy.api.events;

import com.mysticalchemy.recipe.PotionIngredientRecipe;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraftforge.fml.common.eventhandler.Event;

import java.util.HashMap;

/**
 * Base Forge event type for interactions with the alchemy crucible.
 *
 * <p>Subscribe to subclasses of this event to hook into crucible processing stages.</p>
 */
public class CrucibleEvent extends Event {
    /**
     * Fired when an ingredient is about to be applied to an active crucible brew.
     *
     * <p>This event has a result; handlers can allow or deny ingredient processing.</p>
     */
    @HasResult
    public static class AddIngredient extends CrucibleEvent {
        private final HashMap<Potion, Float> effects;
        private final PotionIngredientRecipe recipe;
        private final ItemStack stack;

        /**
         * Creates an ingredient-addition event.
         *
         * @param effects the mutable potion effect map currently tracked by the crucible
         * @param recipe  the matched recipe data for the provided ingredient
         * @param stack   the ingredient stack being inserted
         */
        public AddIngredient(HashMap<Potion, Float> effects, PotionIngredientRecipe recipe, ItemStack stack) {
            this.effects = effects;
            this.recipe = recipe;
            this.stack = stack;
        }

        /**
         * @return the current crucible effect map keyed by potion type
         */
        public HashMap<Potion, Float> getEffects() {
            return effects;
        }

        /**
         * @return the recipe matched for the ingredient being added
         */
        public PotionIngredientRecipe getRecipe() {
            return recipe;
        }

        /**
         * @return the ingredient stack that triggered this event
         */
        public ItemStack getStack() {
            return stack;
        }
    }

    /**
     * Fired when a player extracts potion output from the crucible.
     */
    public static class ExtractPotion extends CrucibleEvent {
        private final ItemStack stack;
        private final EntityPlayer player;

        /**
         * Creates a potion extraction event.
         *
         * @param potionStack the potion container stack being filled or taken
         * @param player      the player performing the extraction action
         */
        public ExtractPotion(ItemStack potionStack, EntityPlayer player) {
            this.stack = potionStack;
            this.player = player;
        }

        /**
         * @return the potion container stack involved in extraction
         */
        public ItemStack getStack() {
            return stack;
        }

        /**
         * @return the player extracting potion from the crucible
         */
        public EntityPlayer getPlayer() {
            return player;
        }
    }
}
