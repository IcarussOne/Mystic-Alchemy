package com.mysticalchemy.registry;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.block.*;
import com.mysticalchemy.tileentity.CrucibleJellyTile;
import com.mysticalchemy.tileentity.CrucibleTile;
import net.minecraft.block.Block;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

@Mod.EventBusSubscriber(modid = MysticAlchemy.MODID)
public final class ModBlocks {
    public static final BlockCrucible CRUCIBLE = new BlockCrucible();
    public static final BlockEmptyCrucible EMPTY_CRUCIBLE = new BlockEmptyCrucible();
    public static final BlockCrucibleWithSlime CRUCIBLE_WITH_SLIME = new BlockCrucibleWithSlime();
    public static final BlockCrucibleWithStone CRUCIBLE_WITH_STONE = new BlockCrucibleWithStone();
    public static final BlockCrucibleWithJelly CRUCIBLE_WITH_JELLY = new BlockCrucibleWithJelly();
    public static final BlockCrucibleMelted CRUCIBLE_MELTED = new BlockCrucibleMelted();

    private ModBlocks() {
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        register(event, CRUCIBLE, "crucible");
        register(event, EMPTY_CRUCIBLE, "crucible_empty");
        register(event, CRUCIBLE_WITH_SLIME, "crucible_with_slime");
        register(event, CRUCIBLE_WITH_STONE, "crucible_with_stone");
        register(event, CRUCIBLE_WITH_JELLY, "crucible_with_jelly");
        register(event, CRUCIBLE_MELTED, "crucible_melted");
    }

    private static void register(RegistryEvent.Register<Block> event, Block block, String name) {
        block.setRegistryName(MysticAlchemy.MODID, name);
        block.setTranslationKey(MysticAlchemy.MODID + "." + name);
        event.getRegistry().register(block);
    }

    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(CrucibleTile.class, new ResourceLocation(MysticAlchemy.MODID, "crucible_tile"));
        GameRegistry.registerTileEntity(CrucibleJellyTile.class, new ResourceLocation(MysticAlchemy.MODID, "crucible_jelly_tile"));
    }
}
