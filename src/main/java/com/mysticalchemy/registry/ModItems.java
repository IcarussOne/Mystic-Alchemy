package com.mysticalchemy.registry;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.item.ItemCrucibleSpoon;
import com.mysticalchemy.item.ItemPotionJellyChunk;
import com.mysticalchemy.item.ItemSamplingKit;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = MysticAlchemy.MODID)
public final class ModItems {
    public static final ItemCrucibleSpoon SPOON = new ItemCrucibleSpoon();
    public static final ItemPotionJellyChunk POTION_JELLY_CHUNK = new ItemPotionJellyChunk();
    public static final ItemSamplingKit SIMPLE_SAMPLING_KIT = new ItemSamplingKit(1.0f);
    public static final ItemSamplingKit ADVANCED_SAMPLING_KIT = new ItemSamplingKit(0.1f);

    private ModItems() {
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        register(event, SPOON, "crucible_spoon");
        register(event, POTION_JELLY_CHUNK, "potion_jelly_chunk");
        register(event, SIMPLE_SAMPLING_KIT, "simple_sampling_kit");
        register(event, ADVANCED_SAMPLING_KIT, "advanced_sampling_kit");
        registerBlockItem(event, ModBlocks.EMPTY_CRUCIBLE, "crucible_empty");
        registerBlockItem(event, ModBlocks.CRUCIBLE_WITH_SLIME, "crucible_with_slime");
        registerBlockItem(event, ModBlocks.CRUCIBLE_WITH_STONE, "crucible_with_stone");
        registerBlockItem(event, ModBlocks.CRUCIBLE_WITH_JELLY, "crucible_with_jelly");
        registerBlockItem(event, ModBlocks.CRUCIBLE_MELTED, "crucible_melted");
    }

    private static void register(RegistryEvent.Register<Item> event, Item item, String name) {
        item.setRegistryName(MysticAlchemy.MODID, name);
        item.setTranslationKey(MysticAlchemy.MODID + "." + name);
        item.setCreativeTab(ModTabs.MYSTIC_ALCHEMY);
        event.getRegistry().register(item);
    }

    private static void registerBlockItem(RegistryEvent.Register<Item> event, Block block, String name) {
        ItemBlock itemBlock = new ItemBlock(block);
        itemBlock.setRegistryName(MysticAlchemy.MODID, name);
        itemBlock.setTranslationKey(block.getTranslationKey());
        itemBlock.setCreativeTab(ModTabs.MYSTIC_ALCHEMY);
        event.getRegistry().register(itemBlock);
    }
}
