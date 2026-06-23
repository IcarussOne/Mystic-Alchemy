package com.mysticalchemy.client;

import com.mysticalchemy.CommonProxy;
import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.client.renderer.CrucibleJellyRenderer;
import com.mysticalchemy.client.renderer.CrucibleRenderer;
import com.mysticalchemy.client.renderer.RenderPotionSlime;
import com.mysticalchemy.entity.EntityPotionSlime;
import com.mysticalchemy.registry.ModBlocks;
import com.mysticalchemy.registry.ModItems;
import com.mysticalchemy.tileentity.CrucibleJellyTile;
import com.mysticalchemy.tileentity.CrucibleTile;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
        ClientRegistry.bindTileEntitySpecialRenderer(CrucibleTile.class, new CrucibleRenderer());
        ClientRegistry.bindTileEntitySpecialRenderer(CrucibleJellyTile.class, new CrucibleJellyRenderer());
        RenderingRegistry.registerEntityRenderingHandler(EntityPotionSlime.class, RenderPotionSlime::new);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, tintIndex) -> {
            if (tintIndex > 0) {
                return 0xFFFFFF;
            }
            return PotionUtils.getPotionColorFromEffectList(ModItems.POTION_JELLY_CHUNK.getStoredEffects(stack));
        }, ModItems.POTION_JELLY_CHUNK);
    }

    @Mod.EventBusSubscriber(modid = MysticAlchemy.MODID, value = Side.CLIENT)
    public static class ModelRegistrar {
        @SubscribeEvent
        public static void onModelRegistry(ModelRegistryEvent event) {
            registerBlockModel(ModBlocks.EMPTY_CRUCIBLE, "crucible_empty");
            registerBlockModel(ModBlocks.CRUCIBLE_WITH_SLIME, "crucible_with_slime");
            registerBlockModel(ModBlocks.CRUCIBLE_WITH_STONE, "crucible_with_stone");
            registerBlockModel(ModBlocks.CRUCIBLE_WITH_JELLY, "crucible_with_jelly");
            registerBlockModel(ModBlocks.CRUCIBLE_MELTED, "crucible_melted");
            registerItemModel(ModItems.SPOON, "crucible_spoon");
            registerItemModel(ModItems.POTION_JELLY_CHUNK, "potion_jelly_chunk");
            registerItemModel(ModItems.SIMPLE_SAMPLING_KIT, "simple_sampling_kit");
            registerItemModel(ModItems.ADVANCED_SAMPLING_KIT, "advanced_sampling_kit");
        }

        @SubscribeEvent
        public static void onTextureStitchPre(TextureStitchEvent.Pre event) {
            event.getMap().registerSprite(new ResourceLocation(MysticAlchemy.MODID, "block/slime"));
        }

        private static void registerBlockModel(Block block, String name) {
            registerItemModel(Item.getItemFromBlock(block), name);
        }

        private static void registerItemModel(Item item, String name) {
            ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(MysticAlchemy.MODID + ":" + name, "inventory"));
        }
    }
}
