package com.mysticalchemy.block;

import com.mysticalchemy.config.BrewingConfig;
import com.mysticalchemy.event.EventDispatcher;
import com.mysticalchemy.recipe.PotionIngredientRecipe;
import com.mysticalchemy.registry.IngredientLoader;
import com.mysticalchemy.registry.ModBlocks;
import com.mysticalchemy.tileentity.CrucibleTile;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.potion.PotionUtils;
import net.minecraft.stats.StatList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Random;

public class BlockCrucible extends BlockCauldron implements ITileEntityProvider {
    public BlockCrucible() {
        setHardness(2.0f);
        setResistance(10.0f);
        setSoundType(SoundType.METAL);
        setDefaultState(blockState.getBaseState().withProperty(LEVEL, Integer.valueOf(3)));
    }

    private static void applyPlayerCrucibleEffects(EntityPlayer player, CrucibleTile crucible) {
        if (player.ticksExisted % 20 != 0) {
            return;
        }

        if (crucible.getTemperature() >= CrucibleTile.BOIL_POINT) {
            player.attackEntityFrom(DamageSource.HOT_FLOOR, 1.0f);
        }

        for (Map.Entry<Potion, Float> entry : crucible.getProminentEffects().entrySet()) {
            Potion potion = entry.getKey();
            if (potion == null || potion.isInstant()) {
                continue;
            }

            int rawAmplifier = Math.max(0, (int) Math.floor(entry.getValue().floatValue()) - 1);
            int amplifier = MathHelper.clamp(rawAmplifier, 0, BrewingConfig.getAmplifierCap(potion.getRegistryName()));
            player.addPotionEffect(new PotionEffect(potion, 20, amplifier, false, true));
        }
    }

    private static void bounceIngredient(EntityItem entityItem) {
        entityItem.motionX = (entityItem.world.rand.nextDouble() - 0.5d) * 0.35d;
        entityItem.motionY = 0.2d + entityItem.world.rand.nextDouble() * 0.15d;
        entityItem.motionZ = (entityItem.world.rand.nextDouble() - 0.5d) * 0.35d;
        entityItem.velocityChanged = true;
    }

    @Nullable
    public static CrucibleTile getCrucibleTile(World world, BlockPos pos) {
        TileEntity tile = world.getTileEntity(pos);
        return tile instanceof CrucibleTile ? (CrucibleTile) tile : null;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World worldIn, int meta) {
        return new CrucibleTile();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        CrucibleTile crucible = getCrucibleTile(world, pos);
        if (crucible == null) {
            return false;
        }

        ItemStack held = player.getHeldItem(hand);
        Item item = held.getItem();

        if (item == Items.WATER_BUCKET) {
            if (!world.isRemote) {
                int level = state.getValue(LEVEL).intValue();
                if (level > 0 && level < 3) {
                    crucible.diluteEffects(0.67f);
                }
                setWaterLevel(world, pos, state, 3);
                player.addStat(StatList.CAULDRON_FILLED);
                if (!player.capabilities.isCreativeMode) {
                    player.setHeldItem(hand, new ItemStack(Items.BUCKET));
                }
            }
            return true;
        }

        if (item == Items.BUCKET) {
            if (!world.isRemote) {
                world.setBlockState(pos, ModBlocks.EMPTY_CRUCIBLE.getDefaultState(), 3);
                player.addStat(StatList.CAULDRON_USED);
                if (!player.capabilities.isCreativeMode) {
                    held.shrink(1);
                    if (held.isEmpty()) {
                        player.setHeldItem(hand, new ItemStack(Items.WATER_BUCKET));
                    } else if (!player.inventory.addItemStackToInventory(new ItemStack(Items.WATER_BUCKET))) {
                        player.dropItem(new ItemStack(Items.WATER_BUCKET), false);
                    }
                }
            }
            return true;
        }

        if (item instanceof ItemPotion && PotionUtils.getPotionFromItem(held) == PotionTypes.WATER) {
            if (!world.isRemote && state.getValue(LEVEL).intValue() < 3) {
                int level = state.getValue(LEVEL).intValue();
                if (level > 0) {
                    crucible.diluteEffects(0.67f);
                }
                setWaterLevel(world, pos, state, level + 1);
                player.addStat(StatList.CAULDRON_USED);
                if (!player.capabilities.isCreativeMode) {
                    player.setHeldItem(hand, new ItemStack(Items.GLASS_BOTTLE));
                }
            }
            return true;
        }

        if (item == Items.GLASS_BOTTLE) {
            return extractPotion(world, pos, state, player, hand, crucible);
        }

        PotionIngredientRecipe recipe = IngredientLoader.findMatchingRecipe(held);
        if (recipe != null) {
            return addIngredient(world, player, hand, crucible, held, recipe);
        }
        if (item == Items.SLIME_BALL) {
            if (world.isRemote) {
                return true;
            }
            if (!crucible.tryAddIngredient(held, 1)) {
                return true;
            }
            world.playSound(null, crucible.getPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.8f, 1.0f);
            held.shrink(1);
            return true;
        }

        return false;
    }

    private boolean extractPotion(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, CrucibleTile crucible) {
        if (world.isRemote || !crucible.canExtractPotion()) {
            return true;
        }

        ItemStack potionStack = crucible.createPotionStack();
        EventDispatcher.dispatchCrucibleExtractPotionEvent(potionStack, player);

        ItemStack held = player.getHeldItem(hand);
        held.shrink(1);
        if (held.isEmpty()) {
            player.setHeldItem(hand, potionStack);
        } else if (!player.inventory.addItemStackToInventory(potionStack)) {
            player.dropItem(potionStack, false);
        }

        setWaterLevel(world, pos, state, state.getValue(LEVEL).intValue() - 1);
        if (state.getValue(LEVEL).intValue() <= 1) {
            world.setBlockState(pos, ModBlocks.EMPTY_CRUCIBLE.getDefaultState(), 3);
        }

        world.playSound(null, pos, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.BLOCKS, 1.0f, 1.0f);
        return true;
    }

    private boolean addIngredient(World world, EntityPlayer player, EnumHand hand, CrucibleTile crucible, ItemStack held, PotionIngredientRecipe recipe) {
        if (world.isRemote) {
            return true;
        }

        if (!crucible.canAddIngredient() && !crucible.hasActiveModifier(recipe, PotionIngredientRecipe.Modifier.SPAWN_SLIME)) {
            return true;
        }

        if (EventDispatcher.dispatchCrucibleAddIngredientEvent(crucible.getEffects(), recipe, held).isCanceled()) {
            return true;
        }

        if (!crucible.tryAddIngredient(held, 1)) {
            return true;
        }

        world.playSound(null, crucible.getPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.8f, 1.0f);
        held.shrink(1);
        return true;
    }

    @Override
    public void onEntityCollision(World world, BlockPos pos, IBlockState state, Entity entityIn) {
        if (world.isRemote) {
            return;
        }

        CrucibleTile crucible = getCrucibleTile(world, pos);
        if (crucible == null) {
            return;
        }

        if (entityIn instanceof EntityPlayer) {
            applyPlayerCrucibleEffects((EntityPlayer) entityIn, crucible);
            return;
        }

        if (!(entityIn instanceof EntityItem)) {
            return;
        }

        EntityItem entityItem = (EntityItem) entityIn;
        ItemStack stack = entityItem.getItem();
        if (stack.isEmpty()) {
            return;
        }

        PotionIngredientRecipe recipe = IngredientLoader.findMatchingRecipe(stack);
        if (recipe == null) {
            if (stack.getItem() != Items.SLIME_BALL) {
                return;
            }
            if (!crucible.tryAddIngredient(stack, 1)) {
                bounceIngredient(entityItem);
                return;
            }
            stack.shrink(1);
            if (stack.isEmpty()) {
                entityItem.setDead();
            } else {
                entityItem.setItem(stack);
            }
            world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.8f, 1.0f);
            return;
        }

        if (EventDispatcher.dispatchCrucibleAddIngredientEvent(crucible.getEffects(), recipe, stack).isCanceled()) {
            return;
        }

        if (!crucible.tryAddIngredient(stack, 1)) {
            bounceIngredient(entityItem);
            return;
        }

        stack.shrink(1);
        if (stack.isEmpty()) {
            entityItem.setDead();
        } else {
            entityItem.setItem(stack);
        }
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.BLOCKS, 0.8f, 1.0f);
    }

    @Override
    public void randomDisplayTick(IBlockState stateIn, World world, BlockPos pos, Random rand) {
        CrucibleTile crucible = getCrucibleTile(world, pos);
        if (crucible == null || !crucible.isActive()) {
            return;
        }

        double centerX = pos.getX() + 0.5d;
        double centerY = pos.getY() + 0.9d;
        double centerZ = pos.getZ() + 0.5d;
        if (crucible.isBoiling()) {
            world.spawnParticle(EnumParticleTypes.SPELL_MOB, centerX, centerY, centerZ, crucible.getColorRed(), crucible.getColorGreen(), crucible.getColorBlue());
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return Item.getItemFromBlock(ModBlocks.EMPTY_CRUCIBLE);
    }

    @Override
    public ItemStack getItem(World worldIn, BlockPos pos, IBlockState state) {
        return new ItemStack(ModBlocks.EMPTY_CRUCIBLE);
    }
}
