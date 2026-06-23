package com.mysticalchemy.entity;

import com.mysticalchemy.config.BrewingConfig;
import com.mysticalchemy.registry.ModItems;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntitySlime;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class EntityPotionSlime extends EntitySlime {
    private static final int HIT_POTION_DURATION_TICKS = 120;
    private static final DataParameter<String> SOURCE_POTION_ID = EntityDataManager.createKey(EntityPotionSlime.class, DataSerializers.STRING);
    private static final DataParameter<Integer> SOURCE_COLOR = EntityDataManager.createKey(EntityPotionSlime.class, DataSerializers.VARINT);
    private static final DataParameter<String> COMBAT_POTION_ID = EntityDataManager.createKey(EntityPotionSlime.class, DataSerializers.STRING);
    private static final DataParameter<Integer> COMBAT_DURATION = EntityDataManager.createKey(EntityPotionSlime.class, DataSerializers.VARINT);
    private static final DataParameter<Integer> COMBAT_AMPLIFIER = EntityDataManager.createKey(EntityPotionSlime.class, DataSerializers.VARINT);
    private final List<PotionEffect> originalEffects = new ArrayList<PotionEffect>();

    public EntityPotionSlime(World worldIn) {
        super(worldIn);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(SOURCE_POTION_ID, "");
        dataManager.register(SOURCE_COLOR, Integer.valueOf(0x7AC74F));
        dataManager.register(COMBAT_POTION_ID, "");
        dataManager.register(COMBAT_DURATION, Integer.valueOf(100));
        dataManager.register(COMBAT_AMPLIFIER, Integer.valueOf(0));
    }

    public void setSourcePotion(@Nullable ResourceLocation potionId, int color) {
        dataManager.set(SOURCE_POTION_ID, potionId == null ? "" : potionId.toString());
        dataManager.set(SOURCE_COLOR, Integer.valueOf(color));
    }

    @Nullable
    public ResourceLocation getSourcePotionId() {
        String potionId = dataManager.get(SOURCE_POTION_ID);
        return potionId == null || potionId.isEmpty() ? null : new ResourceLocation(potionId);
    }

    public int getSourceColor() {
        return dataManager.get(SOURCE_COLOR).intValue();
    }

    public void setMediumSize() {
        setSlimeSize(2, true);
    }

    @Override
    protected EnumParticleTypes getParticleType() {
        return EnumParticleTypes.SPELL_MOB;
    }

    public void setCombatPotion(@Nullable ResourceLocation potionId, int duration, int amplifier) {
        dataManager.set(COMBAT_POTION_ID, potionId == null ? "" : potionId.toString());
        dataManager.set(COMBAT_DURATION, Integer.valueOf(Math.max(20, duration)));
        dataManager.set(COMBAT_AMPLIFIER, Integer.valueOf(Math.max(0, amplifier)));
    }

    @Nullable
    public Potion getCombatPotion() {
        String potionId = dataManager.get(COMBAT_POTION_ID);
        if (potionId == null || potionId.isEmpty()) {
            return null;
        }
        return ForgeRegistries.POTIONS.getValue(new ResourceLocation(potionId));
    }

    private int getCombatDuration() {
        return dataManager.get(COMBAT_DURATION).intValue();
    }

    private int getCombatAmplifier() {
        return dataManager.get(COMBAT_AMPLIFIER).intValue();
    }

    @Nullable
    private ResourceLocation getCombatPotionId() {
        String potionId = dataManager.get(COMBAT_POTION_ID);
        return potionId == null || potionId.isEmpty() ? null : new ResourceLocation(potionId);
    }

    public List<PotionEffect> getOriginalEffects() {
        List<PotionEffect> copy = new ArrayList<PotionEffect>(originalEffects.size());
        for (PotionEffect effect : originalEffects) {
            copy.add(new PotionEffect(effect));
        }
        return copy;
    }

    public void setOriginalEffects(List<PotionEffect> effects) {
        originalEffects.clear();
        if (effects == null) {
            return;
        }
        for (PotionEffect effect : effects) {
            if (effect != null) {
                originalEffects.add(new PotionEffect(effect));
            }
        }
    }

    @Override
    public void onUpdate() {
        boolean wasOnGroundBeforeTick = onGround;
        super.onUpdate();
        if (world.isRemote) {
            if (onGround && !wasOnGroundBeforeTick) {
                spawnPotionLandingParticles();
            }
            return;
        }

        if (ticksExisted % 20 != 0) {
            return;
        }

        Potion potion = getCombatPotion();
        if (potion == null) {
            return;
        }

        applyCombatPotionEffect(this, potion, 40, false);
        AxisAlignedBB aura = getEntityBoundingBox().grow(1.5d);
        List<EntityLivingBase> targets = world.getEntitiesWithinAABB(EntityLivingBase.class, aura, entity -> entity != null && entity != this && !entity.isDead);
        for (EntityLivingBase target : targets) {
            applyCombatPotionEffect(target, potion, Math.max(40, getCombatDuration() / 2), true);
        }
    }

    private void spawnPotionLandingParticles() {
        int size = getSlimeSize();
        int color = getSourceColor();
        double red = ((color >> 16) & 255) / 255.0d;
        double green = ((color >> 8) & 255) / 255.0d;
        double blue = (color & 255) / 255.0d;

        for (int i = 0; i < size * 8; ++i) {
            float angle = rand.nextFloat() * ((float) Math.PI * 2F);
            float spread = rand.nextFloat() * 0.5F + 0.5F;
            float offsetX = net.minecraft.util.math.MathHelper.sin(angle) * size * 0.5F * spread;
            float offsetZ = net.minecraft.util.math.MathHelper.cos(angle) * size * 0.5F * spread;
            world.spawnParticle(
                    EnumParticleTypes.SPELL_MOB,
                    posX + offsetX,
                    getEntityBoundingBox().minY,
                    posZ + offsetZ,
                    red,
                    green,
                    blue
            );
        }
    }

    @Override
    protected void dealDamage(EntityLivingBase entityIn) {
        super.dealDamage(entityIn);
        if (world.isRemote) {
            return;
        }
        Potion potion = getCombatPotion();
        if (potion != null && entityIn != null) {
            applyCombatPotionEffect(entityIn, potion, HIT_POTION_DURATION_TICKS, true);
        }
    }

    private void applyCombatPotionEffect(EntityLivingBase target, Potion potion, int duration, boolean showParticles) {
        if (target == null || potion == null) {
            return;
        }

        if (potion.isInstant()) {
            potion.affectEntity(this, this, target, getCombatAmplifier(), 1.0d);
        } else {
            target.addPotionEffect(new PotionEffect(potion, duration, getCombatAmplifier(), false, showParticles));
        }
    }

    @Override
    protected EntitySlime createInstance() {
        EntityPotionSlime child = new EntityPotionSlime(world);
        child.setSourcePotion(getSourcePotionId(), getSourceColor());
        child.setCombatPotion(getCombatPotionId(), getCombatDuration(), getCombatAmplifier());
        child.setOriginalEffects(getOriginalEffects());
        return child;
    }

    @Override
    protected Item getDropItem() {
        return Items.AIR;
    }

    @Nullable
    protected ResourceLocation getLootTable() {
        return LootTableList.EMPTY;
    }

    @Override
    protected void dropLoot(boolean wasRecentlyHit, int lootingModifier, DamageSource source) {
        super.dropLoot(wasRecentlyHit, lootingModifier, source);
        if (world.isRemote) {
            return;
        }

        float chance = Math.min(1.0f, BrewingConfig.getPotionSlimeJellyDropChance() + lootingModifier * 0.02f);
        if (rand.nextFloat() >= chance) {
            return;
        }

        List<PotionEffect> effects = getOriginalEffects();
        if (effects.isEmpty()) {
            Potion potion = getCombatPotion();
            if (potion != null) {
                effects.add(new PotionEffect(potion, getCombatDuration(), getCombatAmplifier(), false, true));
            }
        }

        ItemStack chunk = new ItemStack(ModItems.POTION_JELLY_CHUNK);
        ModItems.POTION_JELLY_CHUNK.setStoredEffects(chunk, effects);
        entityDropItem(chunk, 0.0f);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        String potionId = dataManager.get(SOURCE_POTION_ID);
        if (potionId != null && !potionId.isEmpty()) {
            compound.setString("SourcePotionId", potionId);
        }
        compound.setInteger("SourceColor", getSourceColor());
        String combatPotionId = dataManager.get(COMBAT_POTION_ID);
        if (combatPotionId != null && !combatPotionId.isEmpty()) {
            compound.setString("CombatPotionId", combatPotionId);
        }
        compound.setInteger("CombatDuration", getCombatDuration());
        compound.setInteger("CombatAmplifier", getCombatAmplifier());
        NBTTagList originalEffectsTag = new NBTTagList();
        for (PotionEffect effect : originalEffects) {
            NBTTagCompound effectTag = new NBTTagCompound();
            effect.writeCustomPotionEffectToNBT(effectTag);
            originalEffectsTag.appendTag(effectTag);
        }
        compound.setTag("OriginalEffects", originalEffectsTag);
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        dataManager.set(SOURCE_POTION_ID, compound.getString("SourcePotionId"));
        dataManager.set(SOURCE_COLOR, Integer.valueOf(compound.getInteger("SourceColor")));
        dataManager.set(COMBAT_POTION_ID, compound.getString("CombatPotionId"));
        dataManager.set(COMBAT_DURATION, Integer.valueOf(compound.hasKey("CombatDuration") ? compound.getInteger("CombatDuration") : 100));
        dataManager.set(COMBAT_AMPLIFIER, Integer.valueOf(compound.hasKey("CombatAmplifier") ? compound.getInteger("CombatAmplifier") : 0));
        originalEffects.clear();
        if (compound.hasKey("OriginalEffects", 9)) {
            NBTTagList list = compound.getTagList("OriginalEffects", 10);
            for (int i = 0; i < list.tagCount(); i++) {
                PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
                if (effect != null) {
                    originalEffects.add(effect);
                }
            }
        }
    }
}
