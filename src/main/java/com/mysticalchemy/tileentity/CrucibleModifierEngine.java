package com.mysticalchemy.tileentity;

import com.mysticalchemy.config.BrewingConfig;
import com.mysticalchemy.entity.EntityPotionSlime;
import com.mysticalchemy.recipe.PotionIngredientRecipe;
import com.mysticalchemy.recipe.PotionIngredientRecipe.Modifier;
import com.mysticalchemy.registry.ModBlocks;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockTallGrass;
import net.minecraft.block.BlockVine;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityAreaEffectCloud;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMagmaCube;
import net.minecraft.init.Blocks;
import net.minecraft.init.MobEffects;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

public final class CrucibleModifierEngine {
    private static final float POTION_BURST_RADIUS = 3.0f;
    private static final float EVAPORATE_RADIUS = 3.0f;
    private static final float EXPLOSION_POWER = 2.0f;
    private static final int EVAPORATE_CLOUD_DURATION = 100;
    private static final HashMap<Modifier, ModifierAction> MODIFIER_ACTIONS = new HashMap<Modifier, ModifierAction>();
    private static final List<Modifier> ACTION_ORDER = Arrays.asList(
            Modifier.POTION_BURST,
            Modifier.EXPLODE,
            Modifier.EVAPORATE_TO_GAS,
            Modifier.BLINDING_FLASH,
            Modifier.IGNITION_BURST,
            Modifier.VERDANT_GROWTH,
            Modifier.SPAWN_MAGMA_CUBE,
            Modifier.SPAWN_SLIME,
            Modifier.TURN_TO_SLIME,
            Modifier.STONIFICATION,
            Modifier.JELLYIFY_POTION,
            Modifier.MELT_CRUCIBLE
    );

    static {
        registerActions();
    }

    private CrucibleModifierEngine() {
    }

    private static void registerAction(Modifier modifier, ModifierAction action) {
        MODIFIER_ACTIONS.put(modifier, action);
    }

    private static void registerActions() {
        registerAction(Modifier.POTION_BURST, (crucible, recipe, quantity) -> {
            if (crucible.tryConsumePotionBurstCooldown()) {
                applyPotionBurst(crucible);
            }
            return false;
        });
        registerAction(Modifier.EXPLODE, (crucible, recipe, quantity) -> {
            if (crucible.tryConsumeExplodeCooldown()) {
                applyExplosion(crucible);
            }
            return false;
        });
        registerAction(Modifier.EVAPORATE_TO_GAS, (crucible, recipe, quantity) -> {
            applyEvaporateToGas(crucible);
            return true;
        });
        registerAction(Modifier.BLINDING_FLASH, (crucible, recipe, quantity) -> {
            applyBlindingFlash(crucible);
            return false;
        });
        registerAction(Modifier.IGNITION_BURST, (crucible, recipe, quantity) -> {
            applyIgnitionBurst(crucible);
            return false;
        });
        registerAction(Modifier.VERDANT_GROWTH, (crucible, recipe, quantity) -> {
            applyVerdantGrowth(crucible);
            return false;
        });
        registerAction(Modifier.SPAWN_MAGMA_CUBE, (crucible, recipe, quantity) -> {
            spawnMagmaCube(crucible);
            return false;
        });
        registerAction(Modifier.SPAWN_SLIME, (crucible, recipe, quantity) -> {
            startPotionSlimeSpawn(crucible);
            return false;
        });
        registerAction(Modifier.TURN_TO_SLIME, (crucible, recipe, quantity) -> {
            convertCrucible(crucible, ModBlocks.CRUCIBLE_WITH_SLIME.getDefaultState());
            return true;
        });
        registerAction(Modifier.STONIFICATION, (crucible, recipe, quantity) -> {
            convertCrucible(crucible, ModBlocks.CRUCIBLE_WITH_STONE.getDefaultState());
            return true;
        });
        registerAction(Modifier.JELLYIFY_POTION, (crucible, recipe, quantity) -> {
            convertCrucibleToJelly(crucible);
            return true;
        });
        registerAction(Modifier.MELT_CRUCIBLE, (crucible, recipe, quantity) -> {
            convertCrucible(crucible, ModBlocks.CRUCIBLE_MELTED.getDefaultState());
            return true;
        });
    }

    public static void onIngredientAdded(CrucibleTile crucible, PotionIngredientRecipe recipe, int quantity) {
        if (recipe == null) {
            return;
        }
        onIngredientAdded(crucible, recipe, recipe.getEnabledModifiers(), quantity);
    }

    public static void onIngredientAdded(CrucibleTile crucible, PotionIngredientRecipe recipe, EnumSet<Modifier> activeModifiers, int quantity) {
        if (crucible == null || activeModifiers == null || quantity <= 0 || activeModifiers.isEmpty()) {
            return;
        }

        World world = crucible.getWorld();
        if (world == null || world.isRemote) {
            return;
        }

        for (Modifier modifier : ACTION_ORDER) {
            if (!activeModifiers.contains(modifier)) {
                continue;
            }

            ModifierAction action = MODIFIER_ACTIONS.get(modifier);
            if (action != null && action.apply(crucible, recipe, quantity)) {
                return;
            }
        }
    }

    public static void applyPotionReverse(CrucibleTile crucible) {
        if (crucible.getEffects().isEmpty()) {
            return;
        }
        crucible.replaceEffects(CrucibleEffectEngine.getReversedEffects(crucible.getEffects()));
    }

    public static HashMap<Potion, Float> getReversedEffects(HashMap<Potion, Float> source) {
        return CrucibleEffectEngine.getReversedEffects(source);
    }

    public static void spawnSlimeFallback(CrucibleTile crucible) {
        if (crucible == null) {
            return;
        }
        startPotionSlimeSpawn(crucible);
    }

    public static void startPotionSlimeSpawn(CrucibleTile crucible) {
        if (crucible == null) {
            return;
        }
        crucible.beginSlimeSpawnAnimation();
    }

    public static boolean spawnPotionSlimeNow(CrucibleTile crucible) {
        return spawnPotionSlime(crucible);
    }

    private static void applyPotionBurst(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }

        List<PotionEffect> effects = crucible.getProminentPotionEffects();
        if (effects.isEmpty()) {
            return;
        }

        AxisAlignedBB area = new AxisAlignedBB(pos).grow(POTION_BURST_RADIUS);
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class, area)) {
            if (target.isDead) {
                continue;
            }
            applyPotionEffects(target, effects);
        }

        world.playEvent(2002, pos, crucible.getPotionColor());
    }

    private static void applyExplosion(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }
        world.createExplosion(null, pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d, EXPLOSION_POWER, false);
    }

    private static void applyEvaporateToGas(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }

        List<PotionEffect> effects = crucible.getProminentPotionEffects();
        EntityAreaEffectCloud cloud = new EntityAreaEffectCloud(world, pos.getX() + 0.5d, pos.getY() + 1.1d, pos.getZ() + 0.5d);
        cloud.setRadius(EVAPORATE_RADIUS);
        cloud.setDuration(EVAPORATE_CLOUD_DURATION);
        cloud.setRadiusPerTick(-cloud.getRadius() / cloud.getDuration());
        cloud.setParticle(net.minecraft.util.EnumParticleTypes.SPELL_MOB);
        for (PotionEffect effect : effects) {
            cloud.addEffect(new PotionEffect(effect));
        }
        world.spawnEntity(cloud);
        world.setBlockState(pos, ModBlocks.EMPTY_CRUCIBLE.getDefaultState(), 3);
    }

    private static void applyBlindingFlash(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }

        float radius = BrewingConfig.getBlindingFlashRadius();
        int duration = BrewingConfig.getBlindingFlashDurationTicks();
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(radius);
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class, area)) {
            target.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, duration, 0, false, true));
            target.addPotionEffect(new PotionEffect(MobEffects.NAUSEA, duration, 0, false, true));
        }

        world.playEvent(2002, pos, 0xFFFFFF);
    }

    private static void applyIgnitionBurst(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }

        float radius = BrewingConfig.getIgnitionBurstRadius();
        int fireSeconds = BrewingConfig.getIgnitionBurstFireSeconds();
        AxisAlignedBB area = new AxisAlignedBB(pos).grow(radius);
        for (EntityLivingBase target : world.getEntitiesWithinAABB(EntityLivingBase.class, area)) {
            target.setFire(fireSeconds);
        }

        BlockPos top = pos.up();
        if (world.isAirBlock(top)) {
            world.setBlockState(top, Blocks.FIRE.getDefaultState(), 3);
        }
    }

    private static void applyVerdantGrowth(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }

        BlockPos top = pos.up();
        if (world.isAirBlock(top) || world.getBlockState(top).getMaterial().isReplaceable()) {
            world.setBlockState(top, Blocks.LEAVES.getDefaultState(), 3);
        }

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                BlockPos ground = pos.add(x, 0, z);
                BlockPos above = ground.up();
                if (!world.isAirBlock(above) || !world.getBlockState(ground).isTopSolid()) {
                    continue;
                }

                int roll = world.rand.nextInt(3);
                if (roll == 0) {
                    world.setBlockState(above, Blocks.TALLGRASS.getDefaultState().withProperty(BlockTallGrass.TYPE, BlockTallGrass.EnumType.GRASS), 3);
                } else {
                    world.setBlockState(above, Blocks.LEAVES.getDefaultState(), 3);
                    BlockPos north = above.north();
                    BlockPos south = above.south();
                    BlockPos east = above.east();
                    BlockPos west = above.west();
                    if (world.isAirBlock(north)) {
                        world.setBlockState(north, Blocks.VINE.getDefaultState().withProperty(BlockVine.SOUTH, Boolean.TRUE), 3);
                    } else if (world.isAirBlock(south)) {
                        world.setBlockState(south, Blocks.VINE.getDefaultState().withProperty(BlockVine.NORTH, Boolean.TRUE), 3);
                    } else if (world.isAirBlock(east)) {
                        world.setBlockState(east, Blocks.VINE.getDefaultState().withProperty(BlockVine.WEST, Boolean.TRUE), 3);
                    } else if (world.isAirBlock(west)) {
                        world.setBlockState(west, Blocks.VINE.getDefaultState().withProperty(BlockVine.EAST, Boolean.TRUE), 3);
                    }
                }
            }
        }
    }

    private static void spawnMagmaCube(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos spawnPos = findSpawnPos(crucible);
        if (world == null || spawnPos == null) {
            return;
        }

        EntityMagmaCube magmaCube = new EntityMagmaCube(world);
        magmaCube.setPosition(spawnPos.getX() + 0.5d, spawnPos.getY(), spawnPos.getZ() + 0.5d);
        world.spawnEntity(magmaCube);
    }

    private static boolean spawnPotionSlime(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        BlockPos spawnPos = findSpawnPos(crucible);
        if (world == null || pos == null || spawnPos == null) {
            return false;
        }

        EntityPotionSlime slime = new EntityPotionSlime(world);
        slime.setMediumSize();
        slime.setPosition(spawnPos.getX() + 0.5d, spawnPos.getY(), spawnPos.getZ() + 0.5d);

        Potion primaryPotion = crucible.getPrimaryPotion();
        ResourceLocation primaryPotionId = primaryPotion != null ? primaryPotion.getRegistryName() : null;
        List<PotionEffect> prominentEffects = crucible.getProminentPotionEffects();
        int sourceColor = primaryPotion != null ? primaryPotion.getLiquidColor() : crucible.getPotionColor();
        slime.setSourcePotion(primaryPotionId, sourceColor);
        slime.setOriginalEffects(prominentEffects);
        if (primaryPotion != null && primaryPotionId != null) {
            slime.setCombatPotion(primaryPotionId, crucible.getDuration(), MathHelper.clamp(crucible.getPrimaryPotionAmplifier(), 0, 3));
        }
        if (world.spawnEntity(slime)) {
            world.setBlockState(pos, ModBlocks.EMPTY_CRUCIBLE.getDefaultState(), 3);
            return true;
        }
        return false;
    }

    @Nullable
    private static BlockPos findSpawnPos(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return null;
        }

        BlockPos[] candidates = new BlockPos[]{
                pos.north(),
                pos.south(),
                pos.east(),
                pos.west(),
                pos.north().east(),
                pos.north().west(),
                pos.south().east(),
                pos.south().west()
        };
        for (BlockPos candidate : candidates) {
            if (world.isAirBlock(candidate.up()) && world.getBlockState(candidate).isTopSolid()) {
                return candidate.up();
            }
        }
        return pos.up();
    }

    private static void convertCrucible(CrucibleTile crucible, IBlockState state) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }

        IBlockState previous = world.getBlockState(pos);
        IBlockState targetState = state;
        if (state.getProperties().containsKey(BlockCauldron.LEVEL) && previous.getProperties().containsKey(BlockCauldron.LEVEL)) {
            int level = previous.getValue(BlockCauldron.LEVEL).intValue();
            int clamped = MathHelper.clamp(level, 1, 3);
            targetState = state.withProperty((IProperty<Integer>) BlockCauldron.LEVEL, Integer.valueOf(clamped));
        }

        world.setBlockState(pos, targetState, 3);
    }

    private static void convertCrucibleToJelly(CrucibleTile crucible) {
        World world = crucible.getWorld();
        BlockPos pos = crucible.getPos();
        if (world == null || pos == null) {
            return;
        }

        List<PotionEffect> storedEffects = crucible.getProminentPotionEffects();
        convertCrucible(crucible, ModBlocks.CRUCIBLE_WITH_JELLY.getDefaultState());
        if (world.getTileEntity(pos) instanceof CrucibleJellyTile) {
            ((CrucibleJellyTile) world.getTileEntity(pos)).setStoredEffects(storedEffects);
        }
    }

    private static void applyPotionEffects(EntityLivingBase target, List<PotionEffect> effects) {
        for (PotionEffect effect : effects) {
            if (effect.getPotion().isInstant()) {
                effect.getPotion().affectEntity(null, null, target, effect.getAmplifier(), 1.0d);
            } else {
                target.addPotionEffect(new PotionEffect(effect));
            }
        }
    }

    private interface ModifierAction {
        boolean apply(CrucibleTile crucible, PotionIngredientRecipe recipe, int quantity);
    }
}
