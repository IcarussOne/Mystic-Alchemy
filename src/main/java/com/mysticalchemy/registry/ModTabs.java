package com.mysticalchemy.registry;

import net.minecraft.item.ItemStack;

public final class ModTabs {
    public static final net.minecraft.creativetab.CreativeTabs MYSTIC_ALCHEMY = new net.minecraft.creativetab.CreativeTabs("mysticalchemy") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(ModBlocks.EMPTY_CRUCIBLE);
        }

        @Override
        public String getTranslationKey() {
            return "itemGroup.mysticalchemy";
        }
    };

    private ModTabs() {
    }
}
