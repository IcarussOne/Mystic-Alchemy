package com.mysticalchemy;

import com.mysticalchemy.registry.IngredientLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = MysticAlchemy.MODID, name = MysticAlchemy.NAME, version = MysticAlchemy.VERSION)
public class MysticAlchemy {
    public static final String MODID = "mysticalchemy";
    public static final String NAME = "Mystic Alchemy Legacy";
    public static final String VERSION = "1.0.0";

    public static final Logger LOGGER = LogManager.getLogger(NAME);

    @SidedProxy(clientSide = "com.mysticalchemy.client.ClientProxy", serverSide = "com.mysticalchemy.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        IngredientLoader.loadPotionIngredientRecipes();
        proxy.postInit(event);
    }
}
