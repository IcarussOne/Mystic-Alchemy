package com.mysticalchemy.tileentity;

import com.mysticalchemy.config.BrewingConfig;
import com.mysticalchemy.recipe.PotionIngredientRecipe;
import com.mysticalchemy.recipe.PotionIngredientRecipe.Modifier;
import com.mysticalchemy.registry.IngredientLoader;
import com.mysticalchemy.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionType;
import net.minecraft.potion.PotionUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import javax.annotation.Nullable;
import java.util.*;

public class CrucibleTile extends TileEntity implements ITickable {
    public static final float MIN_TEMP = 0.0f;
    public static final float MAX_TEMP = 200.0f;
    public static final float BOIL_POINT = 100.0f;
    public static final float ITEM_HEAT_LOSS = 25.0f;
    public static final float DEFAULT_COOL_RATE = 5.0f;
    public static final float STIR_SMOKE_THRESHOLD = 0.25f;
    public static final int MAX_DURATION = 9600;
    private static final int UPDATE_RATE = 10;
    private static final int SMOKE_BURNOFF_TICKS = 50;
    private static final int INITIAL_SMOKE_DELAY_TICKS = 60;
    private static final int SPOON_GRACE_TICKS = 100;
    private static final int MAX_STIR_GRACE_TICKS = 500;
    private static final int DEFAULT_NEAR_SMOKE_TICKS = 30;
    private static final float DEFAULT_LIQUID_SPIN_BOOST = 8.5f;
    private static final float DEFAULT_LIQUID_SPIN_HOLD_DRAG = 0.9995f;
    private static final float DEFAULT_LIQUID_SPIN_NEAR_SMOKE_DRAG = 0.965f;
    private static final float DEFAULT_LIQUID_SPIN_DEFAULT_DRAG = 0.985f;
    private static final float DEFAULT_MAX_LIQUID_SPIN_SPEED = 36.0f;
    private static final float MIN_LIQUID_SPIN_SPEED = 0.02f;
    private static final int DEFAULT_DURATION = 600;
    private static final int INFUSION_TICKS = 100;
    private static final int DEFAULT_COLOR = 0x385DC6;
    private static final float MIN_COLOR_EFFECT_STRENGTH = 1.0f;
    private static final int INSTABILITY_INTERVAL_TICKS = 20;
    private static final int EXPLODE_COOLDOWN_TICKS = 40;
    private static final int POTION_BURST_COOLDOWN_TICKS = 40;
    private static final int SLIME_SPAWN_ANIMATION_TICKS = 60;

    private static final HashMap<Block, Float> HEATERS = new HashMap<Block, Float>();
    private static final HashMap<Biome.TempCategory, Float> BIOME_COOL_RATES = new HashMap<Biome.TempCategory, Float>();

    static {
        HEATERS.put(Blocks.FIRE, 2.0f);
        HEATERS.put(Blocks.FLOWING_LAVA, 5.0f);
        HEATERS.put(Blocks.LAVA, 5.0f);
        HEATERS.put(Blocks.MAGMA, 1.0f);
        HEATERS.put(Blocks.ICE, -2.0f);
        HEATERS.put(Blocks.PACKED_ICE, -2.0f);

        BIOME_COOL_RATES.put(Biome.TempCategory.COLD, 20.0f);
        BIOME_COOL_RATES.put(Biome.TempCategory.MEDIUM, DEFAULT_COOL_RATE);
        BIOME_COOL_RATES.put(Biome.TempCategory.WARM, 0.0f);
        BIOME_COOL_RATES.put(Biome.TempCategory.OCEAN, DEFAULT_COOL_RATE);
    }

    private final CrucibleBrewState brewState = new CrucibleBrewState();
    private float heat = MIN_TEMP;
    private float stir = 1.0f;
    private int smokeTicks;
    private int stirGraceTicks;
    private float liquidSpinAngle;
    private float liquidSpinSpeed;
    private boolean instabilityActive;
    private int instabilityTicks;
    private boolean instabilityNextBadEffect;
    private int instabilityGoodIndex;
    private int instabilityBadIndex;
    private int explodeCooldownTicks;
    private int potionBurstCooldownTicks;
    private int slimeSpawnTicks;
    private Biome.TempCategory biomeTempCategory = null;

    public CrucibleTile() {
        brewState.reset(DEFAULT_DURATION, DEFAULT_COLOR, INFUSION_TICKS);
    }

    private static void setCustomPotionColor(ItemStack stack, int color) {
        NBTTagCompound tag = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
        tag.setInteger("CustomPotionColor", color);
        stack.setTagCompound(tag);
    }

    private static HashMap<Potion, Float> mergeRecipeEffects(HashMap<Potion, Float> fixed, HashMap<Potion, Float> randomized) {
        HashMap<Potion, Float> merged = new HashMap<Potion, Float>();
        if (fixed != null) {
            for (Map.Entry<Potion, Float> entry : fixed.entrySet()) {
                Potion potion = entry.getKey();
                Float magnitude = entry.getValue();
                if (potion != null && magnitude != null) {
                    merged.put(potion, magnitude.floatValue());
                }
            }
        }
        if (randomized != null) {
            for (Map.Entry<Potion, Float> entry : randomized.entrySet()) {
                Potion potion = entry.getKey();
                Float magnitude = entry.getValue();
                if (potion == null || magnitude == null) {
                    continue;
                }
                float current = merged.containsKey(potion) ? merged.get(potion).floatValue() : 0.0f;
                merged.put(potion, current + magnitude.floatValue());
            }
        }
        return merged;
    }

    private static int getAmplifierForMagnitude(Potion potion, float magnitude) {
        int rawAmplifier = Math.max(0, (int) Math.floor(magnitude) - 1);
        int cap = BrewingConfig.getAmplifierCap(potion.getRegistryName());
        return MathHelper.clamp(rawAmplifier, 0, cap);
    }

    @Override
    public void update() {
        if (world == null) {
            return;
        }

        if (brewState.infuseTicks < INFUSION_TICKS) {
            brewState.infuseTicks++;
        }
        if (explodeCooldownTicks > 0) {
            explodeCooldownTicks--;
        }
        if (potionBurstCooldownTicks > 0) {
            potionBurstCooldownTicks--;
        }

        tickLiquidSpin();

        if (slimeSpawnTicks > 0) {
            slimeSpawnTicks = Math.max(0, slimeSpawnTicks - 1);
            if (!world.isRemote && slimeSpawnTicks == 0) {
                CrucibleModifierEngine.spawnPotionSlimeNow(this);
            }
        }

        IBlockState state = world.getBlockState(pos);
        if (!state.getProperties().containsKey(BlockCauldron.LEVEL)) {
            return;
        }

        int level = state.getValue(BlockCauldron.LEVEL).intValue();
        if (world.isRemote) {
            if (level > 0 && !isSlimeSpawnAnimating()) {
                spawnParticles(level);
            }
            return;
        }

        if (isSlimeSpawnAnimating()) {
            return;
        }

        if (world.getTotalWorldTime() % UPDATE_RATE != 0) {
            return;
        }

        if (level <= 0) {
            resetPotion();
            return;
        }

        if (biomeTempCategory == null) {
            biomeTempCategory = world.getBiome(pos).getTempCategory();
        }

        tickHeatAndStir(level, state);
        tickInstability();
    }

    private void spawnParticles(int waterLevel) {
        double particleY = pos.getY() + 0.2d + (0.25d * waterLevel);

        if (heat >= BOIL_POINT) {
            int bubbleCount = Math.max(1, (int) Math.ceil(5.0f * ((heat - BOIL_POINT) / (MAX_TEMP - BOIL_POINT))));
            for (int i = 0; i < bubbleCount; ++i) {
                double particleX = pos.getX() + 0.2d + world.rand.nextDouble() * 0.6d;
                double particleZ = pos.getZ() + 0.2d + world.rand.nextDouble() * 0.6d;
                world.spawnParticle(EnumParticleTypes.WATER_SPLASH, particleX, particleY, particleZ, 0.0d, brewState.splash ? 0.125d : 0.0d, 0.0d);
            }
            spawnColoredEffectParticles(particleY, bubbleCount);
        }

        if (isSmoking()) {
            double particleX = pos.getX() + 0.2d + world.rand.nextDouble() * 0.6d;
            double particleZ = pos.getZ() + 0.2d + world.rand.nextDouble() * 0.6d;
            double smokeProgress = smokeTicks / (double) SMOKE_BURNOFF_TICKS;
            double particleYSpeed = 0.01d + smokeProgress * 0.06d;
            world.spawnParticle(EnumParticleTypes.SMOKE_NORMAL, particleX, particleY, particleZ, 0.0d, particleYSpeed, 0.0d);
        }
    }

    private void spawnColoredEffectParticles(double particleY, int bubbleCount) {
        HashMap<Potion, Float> prominent = getProminentEffects();
        if (prominent.isEmpty()) {
            return;
        }

        List<Potion> potions = new ArrayList<Potion>(prominent.size());
        List<Float> particleWeights = new ArrayList<Float>(prominent.size());
        float totalWeight = 0.0f;
        float weightedMultiplier = 0.0f;

        for (Map.Entry<Potion, Float> entry : prominent.entrySet()) {
            Potion potion = entry.getKey();
            float magnitude = entry.getValue().floatValue();
            if (potion == null || magnitude <= 0.0f) {
                continue;
            }

            int amplifier = getAmplifierForMagnitude(potion, magnitude);
            float particleMultiplier = 1.0f + amplifier * 0.5f;
            float weight = magnitude;

            potions.add(potion);
            particleWeights.add(weight);
            totalWeight += weight;
            weightedMultiplier += weight * particleMultiplier;
        }

        if (potions.isEmpty() || totalWeight <= 0.0f) {
            return;
        }

        int defaultPotionParticles = Math.max(1, Math.round(bubbleCount * 0.2f));
        int coloredParticleCount = Math.max(1, Math.min(bubbleCount - 1, Math.round(defaultPotionParticles * (weightedMultiplier / totalWeight))));
        for (int i = 0; i < coloredParticleCount; i++) {
            Potion potion = pickWeightedPotion(potions, particleWeights, totalWeight);
            int color = potion.getLiquidColor();
            double red = ((color >> 16) & 255) / 255.0d;
            double green = ((color >> 8) & 255) / 255.0d;
            double blue = (color & 255) / 255.0d;
            double particleX = pos.getX() + 0.2d + world.rand.nextDouble() * 0.6d;
            double particleZ = pos.getZ() + 0.2d + world.rand.nextDouble() * 0.6d;
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, particleX, particleY, particleZ, red, green, blue);
        }
    }

    private Potion pickWeightedPotion(List<Potion> potions, List<Float> weights, float totalWeight) {
        float sample = world.rand.nextFloat() * totalWeight;
        float cumulative = 0.0f;
        for (int index = 0; index < potions.size(); index++) {
            cumulative += weights.get(index).floatValue();
            if (sample <= cumulative) {
                return potions.get(index);
            }
        }
        return potions.get(potions.size() - 1);
    }

    public void stir() {
        smokeTicks = 0;
        stirGraceTicks = Math.min(MAX_STIR_GRACE_TICKS, stirGraceTicks + SPOON_GRACE_TICKS);
        stir = 1.0f;
        liquidSpinSpeed = Math.min(DEFAULT_MAX_LIQUID_SPIN_SPEED, liquidSpinSpeed + DEFAULT_LIQUID_SPIN_BOOST);
        markDirty();
        sync();
    }

    public boolean canAddIngredient() {
        return !isSlimeSpawnAnimating() && getWaterLevel() > 0 && heat >= BOIL_POINT;
    }

    public boolean canExtractPotion() {
        return !isSlimeSpawnAnimating() && brewState.infuseTicks >= INFUSION_TICKS && !getProminentEffects().isEmpty();
    }

    public boolean isActive() {
        return getWaterLevel() > 0;
    }

    public boolean isBoiling() {
        return heat >= BOIL_POINT;
    }

    public boolean isSmoking() {
        return isBoiling() && getWaterLevel() > 0 && smokeTicks > 0;
    }

    public boolean isTransitioning() {
        return brewState.infuseTicks < INFUSION_TICKS;
    }

    public boolean tryAddIngredient(ItemStack stack, int quantity) {
        if (isSlimeSpawnAnimating() || stack.isEmpty() || quantity <= 0) {
            return false;
        }

        PotionIngredientRecipe recipe = IngredientLoader.findMatchingRecipe(stack);
        if (recipe == null) {
            if (stack.getItem() == Items.SLIME_BALL) {
                heat = MathHelper.clamp(heat - ITEM_HEAT_LOSS * quantity, MIN_TEMP, MAX_TEMP);
                CrucibleModifierEngine.spawnSlimeFallback(this);
                recordIngredient(stack, quantity);
                markDirty();
                sync();
                return true;
            }
            return false;
        }
        EnumSet<Modifier> activeModifiers = getActiveModifiers(recipe);
        if (!canAddIngredient() && !activeModifiers.contains(Modifier.SPAWN_SLIME)) {
            return false;
        }

        HashMap<Potion, Float> recipeEffects = getRecipeEffects(recipe);
        float potionAllEffectsDelta = getPotionAllEffectsDelta(recipe);
        int potionDuration = getPotionDuration(recipe);
        if (!canMerge(recipeEffects, activeModifiers, quantity, potionAllEffectsDelta, potionDuration)) {
            if (activeModifiers.contains(Modifier.SPAWN_SLIME)) {
                heat = MathHelper.clamp(heat - ITEM_HEAT_LOSS * quantity, MIN_TEMP, MAX_TEMP);
                CrucibleModifierEngine.onIngredientAdded(this, recipe, activeModifiers, quantity);
                recordIngredient(stack, quantity);
                markDirty();
                sync();
                return true;
            }
            return false;
        }

        if (isPotionLingering(recipe)) {
            brewState.lingering = true;
        }
        if (isPotionSplash(recipe)) {
            brewState.splash = true;
        }
        if (potionDuration > 0) {
            brewState.duration = Math.min(MAX_DURATION, brewState.duration + potionDuration * quantity);
        }
        if (isPotionCorruption(recipe)) {
            instabilityActive = true;
        }

        if (activeModifiers.contains(Modifier.POTION_REVERSE)) {
            CrucibleModifierEngine.applyPotionReverse(this);
        }

        heat = MathHelper.clamp(heat - ITEM_HEAT_LOSS * quantity, MIN_TEMP, MAX_TEMP);
        applyAllEffectsDelta(potionAllEffectsDelta, quantity);
        mergeEffects(recipeEffects, quantity);
        CrucibleModifierEngine.onIngredientAdded(this, recipe, activeModifiers, quantity);
        recordIngredient(stack, quantity);
        recalculatePotionColor();
        markDirty();
        sync();
        return true;
    }

    public ItemStack createPotionStack() {
        ItemStack stack = new ItemStack(Items.POTIONITEM);
        if (brewState.splash) {
            stack = new ItemStack(Items.SPLASH_POTION);
        } else if (brewState.lingering) {
            stack = new ItemStack(Items.LINGERING_POTION);
        }

        List<PotionEffect> effects = createProminentPotionEffects();

        if (effects.size() == 1) {
            PotionType vanillaType = CruciblePotionNaming.findVanillaNamedType(effects.get(0));
            if (vanillaType != null) {
                stack.setTranslatableName(vanillaType.getNamePrefixed(CruciblePotionNaming.getPotionNamePrefix(brewState.splash, brewState.lingering)));
            }
        }

        if (!effects.isEmpty()) {
            PotionUtils.appendEffects(stack, effects);
            setCustomPotionColor(stack, PotionUtils.getPotionColorFromEffectList(effects));
            String displayName = CruciblePotionNaming.generateDisplayName(effects, brewState.splash, brewState.lingering);
            if (displayName != null && !displayName.isEmpty()) {
                stack.setStackDisplayName(displayName);
            }
        }

        return stack;
    }

    public HashMap<Potion, Float> getEffects() {
        return brewState.effectStrengths;
    }

    public HashMap<Potion, Float> getProminentEffects() {
        HashMap<Potion, Float> prominent = new HashMap<Potion, Float>();
        for (Map.Entry<Potion, Float> entry : brewState.effectStrengths.entrySet()) {
            Potion potion = entry.getKey();
            float magnitude = entry.getValue().floatValue();
            if (potion != null && magnitude >= 1.0f && !BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                prominent.put(potion, magnitude);
            }
        }
        return prominent;
    }

    public int getDuration() {
        return brewState.duration;
    }

    public int getRemainingTicks() {
        return brewState.duration;
    }

    public float getTemperature() {
        return heat;
    }

    public void applyRecipe(PotionIngredientRecipe recipe) {
        if (isSlimeSpawnAnimating() || recipe == null || !canAddIngredient()) {
            return;
        }

        EnumSet<Modifier> activeModifiers = getActiveModifiers(recipe);
        HashMap<Potion, Float> recipeEffects = getRecipeEffects(recipe);
        float potionAllEffectsDelta = getPotionAllEffectsDelta(recipe);
        int potionDuration = getPotionDuration(recipe);
        if (!canMerge(recipeEffects, activeModifiers, 1, potionAllEffectsDelta, potionDuration)) {
            return;
        }

        if (isPotionLingering(recipe)) {
            brewState.lingering = true;
        }
        if (isPotionSplash(recipe)) {
            brewState.splash = true;
        }
        if (potionDuration > 0) {
            brewState.duration = Math.min(MAX_DURATION, brewState.duration + potionDuration);
        }
        if (isPotionCorruption(recipe)) {
            instabilityActive = true;
        }

        if (activeModifiers.contains(Modifier.POTION_REVERSE)) {
            CrucibleModifierEngine.applyPotionReverse(this);
        }

        heat = MathHelper.clamp(heat - ITEM_HEAT_LOSS, MIN_TEMP, MAX_TEMP);
        applyAllEffectsDelta(potionAllEffectsDelta, 1);
        mergeEffects(recipeEffects, 1);
        CrucibleModifierEngine.onIngredientAdded(this, recipe, activeModifiers, 1);
        recalculatePotionColor();
        markDirty();
        sync();
    }

    public float getHeat() {
        return heat;
    }

    public float getMaxHeat() {
        return MAX_TEMP;
    }

    public float getStir() {
        return stir;
    }

    public float getColorRed() {
        return ((getPotionColor() >> 16) & 255) / 255.0f;
    }

    public float getColorGreen() {
        return ((getPotionColor() >> 8) & 255) / 255.0f;
    }

    public float getColorBlue() {
        return (getPotionColor() & 255) / 255.0f;
    }

    public float getLiquidSpinAngle(float partialTicks) {
        return liquidSpinAngle + liquidSpinSpeed * partialTicks;
    }

    public float getLiquidSpinStrength() {
        return MathHelper.clamp(liquidSpinSpeed / DEFAULT_MAX_LIQUID_SPIN_SPEED, 0.0f, 1.0f);
    }

    public int getPotionColor() {
        if (brewState.infuseTicks >= INFUSION_TICKS) {
            return brewState.targetColor;
        }

        float pct = brewState.infuseTicks / (float) INFUSION_TICKS;
        int startRed = (brewState.startColor >> 16) & 255;
        int startGreen = (brewState.startColor >> 8) & 255;
        int startBlue = brewState.startColor & 255;
        int targetRed = (brewState.targetColor >> 16) & 255;
        int targetGreen = (brewState.targetColor >> 8) & 255;
        int targetBlue = brewState.targetColor & 255;

        int red = startRed + Math.round((targetRed - startRed) * pct);
        int green = startGreen + Math.round((targetGreen - startGreen) * pct);
        int blue = startBlue + Math.round((targetBlue - startBlue) * pct);
        return (red << 16) | (green << 8) | blue;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setFloat("Heat", heat);
        compound.setFloat("Stir", stir);
        compound.setInteger("SmokeTicks", smokeTicks);
        compound.setInteger("StirGraceTicks", stirGraceTicks);
        compound.setFloat("LiquidSpinAngle", liquidSpinAngle);
        compound.setFloat("LiquidSpinSpeed", liquidSpinSpeed);
        compound.setBoolean("InstabilityActive", instabilityActive);
        compound.setInteger("InstabilityTicks", instabilityTicks);
        compound.setBoolean("InstabilityNextBadEffect", instabilityNextBadEffect);
        compound.setInteger("InstabilityGoodIndex", instabilityGoodIndex);
        compound.setInteger("InstabilityBadIndex", instabilityBadIndex);
        compound.setInteger("ExplodeCooldownTicks", explodeCooldownTicks);
        compound.setInteger("PotionBurstCooldownTicks", potionBurstCooldownTicks);
        compound.setInteger("SlimeSpawnTicks", slimeSpawnTicks);
        CrucibleNbtCodec.writeBrewState(brewState, compound);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        heat = compound.getFloat("Heat");
        stir = compound.hasKey("Stir") ? compound.getFloat("Stir") : 1.0f;
        smokeTicks = compound.hasKey("SmokeTicks") ? compound.getInteger("SmokeTicks") : 0;
        stirGraceTicks = compound.hasKey("StirGraceTicks") ? compound.getInteger("StirGraceTicks") : 0;
        liquidSpinAngle = compound.hasKey("LiquidSpinAngle") ? compound.getFloat("LiquidSpinAngle") : 0.0f;
        liquidSpinSpeed = compound.hasKey("LiquidSpinSpeed") ? compound.getFloat("LiquidSpinSpeed") : 0.0f;
        instabilityActive = compound.getBoolean("InstabilityActive");
        instabilityTicks = compound.hasKey("InstabilityTicks") ? compound.getInteger("InstabilityTicks") : 0;
        instabilityNextBadEffect = compound.getBoolean("InstabilityNextBadEffect");
        instabilityGoodIndex = compound.hasKey("InstabilityGoodIndex") ? compound.getInteger("InstabilityGoodIndex") : 0;
        instabilityBadIndex = compound.hasKey("InstabilityBadIndex") ? compound.getInteger("InstabilityBadIndex") : 0;
        explodeCooldownTicks = compound.hasKey("ExplodeCooldownTicks") ? compound.getInteger("ExplodeCooldownTicks") : 0;
        potionBurstCooldownTicks = compound.hasKey("PotionBurstCooldownTicks") ? compound.getInteger("PotionBurstCooldownTicks") : 0;
        slimeSpawnTicks = compound.hasKey("SlimeSpawnTicks") ? compound.getInteger("SlimeSpawnTicks") : 0;
        CrucibleNbtCodec.readBrewState(brewState, compound, DEFAULT_DURATION, DEFAULT_COLOR, INFUSION_TICKS);
        biomeTempCategory = null;
        recalculatePotionColor();
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Nullable
    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, 0, getUpdateTag());
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        readFromNBT(pkt.getNbtCompound());
    }

    @Override
    public boolean shouldRefresh(World world, BlockPos pos, IBlockState oldState, IBlockState newState) {
        return oldState.getBlock() != newState.getBlock();
    }

    private void tickHeatAndStir(int level, IBlockState state) {
        float previousHeat = heat;
        float previousStir = stir;
        int previousSmokeTicks = smokeTicks;
        int previousStirGraceTicks = stirGraceTicks;
        Block below = world.getBlockState(pos.down()).getBlock();
        if (HEATERS.containsKey(below)) {
            heat = MathHelper.clamp(heat + HEATERS.get(below).floatValue(), MIN_TEMP, MAX_TEMP);
        } else {
            float coolTarget = BIOME_COOL_RATES.containsKey(biomeTempCategory) ? BIOME_COOL_RATES.get(biomeTempCategory).floatValue() : DEFAULT_COOL_RATE;
            if (heat > coolTarget) {
                heat = Math.max(coolTarget, heat - DEFAULT_COOL_RATE);
            } else {
                heat = coolTarget;
            }
        }

        boolean wasBoiling = previousHeat >= BOIL_POINT;
        boolean isBoilingNow = heat >= BOIL_POINT;

        if (isBoilingNow && !wasBoiling && smokeTicks == 0 && stirGraceTicks == 0) {
            stirGraceTicks = INITIAL_SMOKE_DELAY_TICKS;
        }

        if (isBoilingNow) {
            if (stirGraceTicks > 0) {
                stirGraceTicks = Math.max(0, stirGraceTicks - UPDATE_RATE);
                smokeTicks = 0;
                stir = 1.0f;
            } else {
                smokeTicks = Math.min(SMOKE_BURNOFF_TICKS, smokeTicks + UPDATE_RATE);
                stir = MathHelper.clamp(1.0f - (smokeTicks / (float) SMOKE_BURNOFF_TICKS), 0.0f, 1.0f);
            }
        } else {
            smokeTicks = 0;
            stirGraceTicks = 0;
            stir = 1.0f;
        }

        boolean changed = previousHeat != heat;
        if (smokeTicks >= SMOKE_BURNOFF_TICKS) {
            lowerWaterLevel(level, state);
            smokeTicks = 0;
            stir = 1.0f;
            changed = true;
        }

        changed = changed || previousStir != stir || previousSmokeTicks != smokeTicks || previousStirGraceTicks != stirGraceTicks;

        if (changed) {
            markDirty();
            sync();
        }
    }

    private void lowerWaterLevel(int level, IBlockState state) {
        if (world == null) {
            return;
        }

        if (level <= 1) {
            world.setBlockState(pos, ModBlocks.EMPTY_CRUCIBLE.getDefaultState(), 3);
            resetPotion();
            return;
        }

        world.setBlockState(pos, state.withProperty(BlockCauldron.LEVEL, Integer.valueOf(level - 1)), 3);
    }

    private void resetPotion() {
        heat = MIN_TEMP;
        smokeTicks = 0;
        stirGraceTicks = 0;
        stir = 1.0f;
        liquidSpinAngle = 0.0f;
        liquidSpinSpeed = 0.0f;
        brewState.reset(DEFAULT_DURATION, DEFAULT_COLOR, INFUSION_TICKS);
        instabilityActive = false;
        instabilityTicks = 0;
        instabilityNextBadEffect = false;
        instabilityGoodIndex = 0;
        instabilityBadIndex = 0;
        explodeCooldownTicks = 0;
        potionBurstCooldownTicks = 0;
        slimeSpawnTicks = 0;
        markDirty();
        sync();
    }

    private void tickLiquidSpin() {
        liquidSpinAngle += liquidSpinSpeed;
        boolean boiling = heat >= BOIL_POINT;
        boolean nearSmoke = boiling && (smokeTicks > 0 || stirGraceTicks <= DEFAULT_NEAR_SMOKE_TICKS);
        if (boiling && !nearSmoke) {
            liquidSpinSpeed *= DEFAULT_LIQUID_SPIN_HOLD_DRAG;
        } else if (nearSmoke) {
            liquidSpinSpeed *= DEFAULT_LIQUID_SPIN_NEAR_SMOKE_DRAG;
        } else {
            liquidSpinSpeed *= DEFAULT_LIQUID_SPIN_DEFAULT_DRAG;
        }

        liquidSpinSpeed = MathHelper.clamp(liquidSpinSpeed, -DEFAULT_MAX_LIQUID_SPIN_SPEED, DEFAULT_MAX_LIQUID_SPIN_SPEED);
        if (Math.abs(liquidSpinSpeed) < MIN_LIQUID_SPIN_SPEED) {
            liquidSpinSpeed = 0.0f;
        }
    }

    private void recalculatePotionColor() {
        List<PotionEffect> effects = createColorPotionEffects();
        if (effects.isEmpty()) {
            brewState.startColor = getPotionColor();
            brewState.targetColor = DEFAULT_COLOR;
            brewState.infuseTicks = INFUSION_TICKS;
            return;
        }

        int newColor = PotionUtils.getPotionColorFromEffectList(effects);
        if (newColor != brewState.targetColor) {
            brewState.startColor = getPotionColor();
            brewState.targetColor = newColor;
            brewState.infuseTicks = 0;
        }
    }

    private List<PotionEffect> createColorPotionEffects() {
        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        for (Map.Entry<Potion, Float> entry : brewState.effectStrengths.entrySet()) {
            Potion potion = entry.getKey();
            float magnitude = entry.getValue().floatValue();
            if (potion == null || magnitude < MIN_COLOR_EFFECT_STRENGTH || BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                continue;
            }
            int amplifier = getAmplifierForMagnitude(potion, magnitude);
            effects.add(new PotionEffect(potion, brewState.duration, amplifier, false, true));
        }
        return effects;
    }

    private List<PotionEffect> createProminentPotionEffects() {
        List<PotionEffect> effects = new ArrayList<PotionEffect>();
        for (Map.Entry<Potion, Float> entry : getProminentEffects().entrySet()) {
            Potion potion = entry.getKey();
            if (potion == null) {
                continue;
            }
            int amplifier = getAmplifierForMagnitude(potion, entry.getValue().floatValue());
            effects.add(new PotionEffect(potion, brewState.duration, amplifier, false, true));
        }
        return effects;
    }

    private boolean canMerge(HashMap<Potion, Float> recipeEffects, EnumSet<Modifier> activeModifiers, int quantity, float potionAllEffectsDelta, int potionDuration) {
        return CrucibleEffectEngine.canMerge(
                brewState.effectStrengths,
                activeModifiers.contains(Modifier.POTION_REVERSE),
                potionAllEffectsDelta,
                recipeEffects,
                quantity,
                BrewingConfig.getMaxEffectsAboveOne(),
                brewState.duration,
                potionDuration,
                MAX_DURATION
        );
    }

    private long getWorldSeed() {
        long worldSeed = world != null ? world.getSeed() : 0L;
        return BrewingConfig.getRandomizationSeed(worldSeed);
    }

    private HashMap<Potion, Float> getRecipeEffects(PotionIngredientRecipe recipe) {
        if (!isRecipePropertyEnabled(recipe)) {
            return new HashMap<Potion, Float>();
        }
        BrewingConfig.RandomizationMode mode = BrewingConfig.getRandomizationMode();
        switch (mode) {
            case OFF:
                return recipe.getPotionEffects();
            case MERGE:
                return mergeRecipeEffects(recipe.getPotionEffects(), recipe.getRandomizedEffects(getWorldSeed()));
            case REPLACE:
            default:
                return recipe.getRandomizedEffects(getWorldSeed());
        }
    }

    public boolean hasActiveModifier(PotionIngredientRecipe recipe, Modifier modifier) {
        return getActiveModifiers(recipe).contains(modifier);
    }

    private EnumSet<Modifier> getActiveModifiers(PotionIngredientRecipe recipe) {
        if (recipe == null) {
            return EnumSet.noneOf(Modifier.class);
        }
        if (!isRecipePropertyEnabled(recipe)) {
            return EnumSet.noneOf(Modifier.class);
        }

        BrewingConfig.RandomizationMode mode = BrewingConfig.getRandomizationMode();
        switch (mode) {
            case OFF:
                return recipe.getEnabledModifiers();
            case MERGE:
                EnumSet<Modifier> merged = recipe.getEnabledModifiers();
                if (merged.isEmpty()) {
                    merged.addAll(recipe.getRandomizedModifiersForReplaceMode(getWorldSeed()));
                } else {
                    merged.addAll(recipe.getRandomizedModifiers(getWorldSeed()));
                }
                return merged;
            case REPLACE:
            default:
                return recipe.getRandomizedModifiersForReplaceMode(getWorldSeed());
        }
    }

    private boolean isRecipePropertyEnabled(PotionIngredientRecipe recipe) {
        if (recipe == null) {
            return false;
        }

        BrewingConfig.RandomizationMode mode = BrewingConfig.getRandomizationMode();
        if (mode == BrewingConfig.RandomizationMode.OFF) {
            return true;
        }

        int coveragePercent = BrewingConfig.getRandomizedPropertyCoveragePercent();
        return IngredientLoader.isRandomizedPropertyEnabled(recipe, getWorldSeed(), coveragePercent);
    }

    private float getPotionAllEffectsDelta(PotionIngredientRecipe recipe) {
        if (recipe == null || !isRecipePropertyEnabled(recipe)) {
            return 0.0f;
        }
        return recipe.getPotionAllEffectsDelta();
    }

    private int getPotionDuration(PotionIngredientRecipe recipe) {
        if (recipe == null || !isRecipePropertyEnabled(recipe)) {
            return 0;
        }
        return recipe.getPotionDuration();
    }

    private boolean isPotionCorruption(PotionIngredientRecipe recipe) {
        return recipe != null && isRecipePropertyEnabled(recipe) && recipe.isPotionCorruption();
    }

    private boolean isPotionSplash(PotionIngredientRecipe recipe) {
        return recipe != null && isRecipePropertyEnabled(recipe) && recipe.isPotionSplash();
    }

    private boolean isPotionLingering(PotionIngredientRecipe recipe) {
        return recipe != null && isRecipePropertyEnabled(recipe) && recipe.isPotionLingering();
    }

    private void applyAllEffectsDelta(float delta, int quantity) {
        CrucibleEffectEngine.applyAllEffectsDelta(brewState.effectStrengths, delta, quantity);
    }

    private void mergeEffects(HashMap<Potion, Float> effects, int quantity) {
        CrucibleEffectEngine.mergeEffects(brewState.effectStrengths, effects, quantity);
    }

    private void tickInstability() {
        CrucibleEffectEngine.InstabilityState state = CrucibleEffectEngine.tickInstability(
                brewState.effectStrengths,
                instabilityActive,
                heat,
                BOIL_POINT,
                UPDATE_RATE,
                instabilityTicks,
                INSTABILITY_INTERVAL_TICKS,
                instabilityNextBadEffect,
                instabilityGoodIndex,
                instabilityBadIndex
        );
        instabilityTicks = state.instabilityTicks;
        instabilityNextBadEffect = state.instabilityNextBadEffect;
        instabilityGoodIndex = state.instabilityGoodIndex;
        instabilityBadIndex = state.instabilityBadIndex;

        if (state.changed) {
            recalculatePotionColor();
            markDirty();
            sync();
        }
    }

    public HashMap<String, Integer> getTrackedIngredients() {
        return new HashMap<String, Integer>(brewState.ingredientCounts);
    }

    private void recordIngredient(ItemStack stack, int quantity) {
        if (stack.isEmpty() || quantity <= 0 || stack.getItem().getRegistryName() == null) {
            return;
        }

        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(stack.getItem().getRegistryName().toString());
        keyBuilder.append("@").append(stack.getMetadata());
        if (stack.hasTagCompound()) {
            keyBuilder.append(stack.getTagCompound().toString());
        }

        String key = keyBuilder.toString();
        int current = brewState.ingredientCounts.containsKey(key) ? brewState.ingredientCounts.get(key).intValue() : 0;
        brewState.ingredientCounts.put(key, current + quantity);
    }

    public void diluteEffects(float retainedFactor) {
        float factor = MathHelper.clamp(retainedFactor, 0.0f, 1.0f);
        if (brewState.effectStrengths.isEmpty()) {
            return;
        }

        for (Map.Entry<Potion, Float> entry : brewState.effectStrengths.entrySet()) {
            entry.setValue(entry.getValue().floatValue() * factor);
        }

        recalculatePotionColor();
        markDirty();
        sync();
    }

    private int getWaterLevel() {
        if (world == null) {
            return 0;
        }

        IBlockState state = world.getBlockState(pos);
        if (!state.getProperties().containsKey(BlockCauldron.LEVEL)) {
            return 0;
        }
        return state.getValue(BlockCauldron.LEVEL).intValue();
    }

    private void sync() {
        if (world != null) {
            IBlockState state = world.getBlockState(pos);
            world.notifyBlockUpdate(pos, state, state, 3);
        }
    }

    public List<PotionEffect> getProminentPotionEffects() {
        return createProminentPotionEffects();
    }

    @Nullable
    public Potion getPrimaryPotion() {
        Potion selected = null;
        float highestMagnitude = 0.0f;
        for (Map.Entry<Potion, Float> entry : brewState.effectStrengths.entrySet()) {
            Potion potion = entry.getKey();
            if (potion == null || potion.getRegistryName() == null || BrewingConfig.isEffectDisabled(potion.getRegistryName())) {
                continue;
            }
            float magnitude = entry.getValue().floatValue();
            if (magnitude > highestMagnitude) {
                highestMagnitude = magnitude;
                selected = potion;
            }
        }
        return selected;
    }

    public int getPrimaryPotionAmplifier() {
        Potion primary = getPrimaryPotion();
        if (primary == null) {
            return 0;
        }
        Float magnitude = brewState.effectStrengths.get(primary);
        if (magnitude == null) {
            return 0;
        }
        return getAmplifierForMagnitude(primary, magnitude.floatValue());
    }

    public void replaceEffects(HashMap<Potion, Float> replacements) {
        brewState.effectStrengths.clear();
        if (replacements != null) {
            brewState.effectStrengths.putAll(replacements);
        }
        recalculatePotionColor();
        markDirty();
        sync();
    }

    public boolean tryConsumeExplodeCooldown() {
        if (explodeCooldownTicks > 0) {
            return false;
        }
        explodeCooldownTicks = EXPLODE_COOLDOWN_TICKS;
        markDirty();
        return true;
    }

    public boolean tryConsumePotionBurstCooldown() {
        if (potionBurstCooldownTicks > 0) {
            return false;
        }
        potionBurstCooldownTicks = POTION_BURST_COOLDOWN_TICKS;
        markDirty();
        return true;
    }

    public void beginSlimeSpawnAnimation() {
        if (world == null || world.isRemote || getWaterLevel() <= 0) {
            return;
        }
        slimeSpawnTicks = SLIME_SPAWN_ANIMATION_TICKS;
        smokeTicks = 0;
        stirGraceTicks = 0;
        liquidSpinSpeed = 0.0f;
        markDirty();
        sync();
    }

    public boolean isSlimeSpawnAnimating() {
        return slimeSpawnTicks > 0;
    }

    public float getSlimeSpawnAnimationProgress(float partialTicks) {
        if (slimeSpawnTicks <= 0) {
            return 0.0f;
        }
        float ticksRemaining = Math.max(0.0f, slimeSpawnTicks - partialTicks);
        float elapsed = SLIME_SPAWN_ANIMATION_TICKS - ticksRemaining;
        return MathHelper.clamp(elapsed / SLIME_SPAWN_ANIMATION_TICKS, 0.0f, 1.0f);
    }
}
