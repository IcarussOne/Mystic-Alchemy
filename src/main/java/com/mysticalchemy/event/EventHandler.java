package com.mysticalchemy.event;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.config.Config;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraftforge.event.brewing.PotionBrewEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MysticAlchemy.MODID)
public final class EventHandler {
    private EventHandler() {
    }

    @SubscribeEvent
    public static void onPotionBrew(PotionBrewEvent.Post event) {
        if (!Config.brewing.ma_disable_vanilla_brewing) {
            return;
        }

        for (int slot = 0; slot < event.getLength(); slot++) {
            ItemStack stack = event.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }

            if (stack.getItem() == Items.POTIONITEM
                    || stack.getItem() == Items.SPLASH_POTION
                    || stack.getItem() == Items.LINGERING_POTION) {
                event.setItem(slot, PotionUtils.addPotionToItemStack(new ItemStack(stack.getItem()), PotionTypes.AWKWARD));
            }
        }
    }
}
