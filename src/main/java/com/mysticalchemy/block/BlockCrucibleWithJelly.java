package com.mysticalchemy.block;

import com.mysticalchemy.registry.ModBlocks;
import com.mysticalchemy.registry.ModItems;
import com.mysticalchemy.tileentity.CrucibleJellyTile;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockCrucibleWithJelly extends BlockCrucibleVariant {
    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(World world, IBlockState state) {
        return new CrucibleJellyTile();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() != ModItems.SPOON) {
            return false;
        }

        if (!world.isRemote) {
            ItemStack chunks = new ItemStack(ModItems.POTION_JELLY_CHUNK, 3);
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof CrucibleJellyTile) {
                ModItems.POTION_JELLY_CHUNK.setStoredEffects(chunks, ((CrucibleJellyTile) tile).getStoredEffects());
            }
            if (!player.inventory.addItemStackToInventory(chunks)) {
                player.dropItem(chunks, false);
            }

            if (!player.capabilities.isCreativeMode) {
                held.damageItem(1, player);
            }

            world.setBlockState(pos, ModBlocks.EMPTY_CRUCIBLE.getDefaultState(), 3);
            world.removeTileEntity(pos);
            world.playEvent(2005, pos, 0);
        }

        return true;
    }
}
