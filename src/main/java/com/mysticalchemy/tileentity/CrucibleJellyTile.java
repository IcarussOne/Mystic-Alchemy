package com.mysticalchemy.tileentity;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.potion.PotionEffect;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CrucibleJellyTile extends TileEntity {
    private final List<PotionEffect> storedEffects = new ArrayList<PotionEffect>();

    public List<PotionEffect> getStoredEffects() {
        List<PotionEffect> copy = new ArrayList<PotionEffect>(storedEffects.size());
        for (PotionEffect effect : storedEffects) {
            copy.add(new PotionEffect(effect));
        }
        return copy;
    }

    public void setStoredEffects(List<PotionEffect> effects) {
        storedEffects.clear();
        if (effects != null) {
            for (PotionEffect effect : effects) {
                if (effect != null) {
                    storedEffects.add(new PotionEffect(effect));
                }
            }
        }
        markDirty();
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        NBTTagList list = new NBTTagList();
        for (PotionEffect effect : storedEffects) {
            NBTTagCompound effectTag = new NBTTagCompound();
            effect.writeCustomPotionEffectToNBT(effectTag);
            list.appendTag(effectTag);
        }
        compound.setTag("StoredEffects", list);
        return compound;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        storedEffects.clear();
        if (!compound.hasKey("StoredEffects", 9)) {
            return;
        }

        NBTTagList list = compound.getTagList("StoredEffects", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            PotionEffect effect = PotionEffect.readCustomPotionEffectFromNBT(list.getCompoundTagAt(i));
            if (effect != null) {
                storedEffects.add(effect);
            }
        }
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
}
