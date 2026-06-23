package com.mysticalchemy.registry;

import com.mysticalchemy.MysticAlchemy;
import com.mysticalchemy.entity.EntityPotionSlime;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityEntry;
import net.minecraftforge.fml.common.registry.EntityEntryBuilder;

@Mod.EventBusSubscriber(modid = MysticAlchemy.MODID)
public final class ModEntities {
    private static int nextEntityId = 0;

    private ModEntities() {
    }

    @SubscribeEvent
    public static void registerEntities(RegistryEvent.Register<EntityEntry> event) {
        register(event, "potion_slime", EntityPotionSlime.class, 0x7AC74F, 0x3A4D2F);
    }

    private static void register(RegistryEvent.Register<EntityEntry> event, String name, Class<? extends net.minecraft.entity.Entity> entityClass, int eggPrimaryColor, int eggSecondaryColor) {
        ResourceLocation id = new ResourceLocation(MysticAlchemy.MODID, name);
        EntityEntry entry = EntityEntryBuilder.create()
                .entity(entityClass)
                .id(id, nextEntityId++)
                .name(id.toString())
                .tracker(64, 3, true)
                .egg(eggPrimaryColor, eggSecondaryColor)
                .build();
        event.getRegistry().register(entry);
    }
}
