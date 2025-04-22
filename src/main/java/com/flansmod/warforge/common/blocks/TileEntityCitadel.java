package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import java.util.UUID;

public class TileEntityCitadel extends TileEntityYieldCollector implements IClaim {
    public static final int BANNER_SLOT_INDEX = NUM_BASE_SLOTS;
    public static final int NUM_SLOTS = NUM_BASE_SLOTS + 1;

    public UUID placer = Faction.nullUuid;

    // The banner stack is an optional slot that sets all banners in owned chunks to copy the design
    protected ItemStack bannerStack;

    public TileEntityCitadel() {
        bannerStack = ItemStack.EMPTY;
    }

    public void onPlacedBy(EntityLivingBase placer) {
        // This locks in the placer as the only person who can create a faction using the interface on this citadel
        this.placer = placer.getUniqueID();
    }

    // IClaim
    @Override
    public int getDefenceStrength() {
        return WarForgeConfig.CLAIM_STRENGTH_CITADEL;
    }

    @Override
    public int getSupportStrength() {
        return WarForgeConfig.SUPPORT_STRENGTH_CITADEL;
    }

    @Override
    public int getAttackStrength() {
        return 0;
    }

    @Override
    protected float getYieldMultiplier() {
        return 2.0f;
    }

    @Override
    public String getClaimDisplayName() {
        if (factionName == null || factionName.isEmpty()) {
            return "Unclaimed Citadel";
        }
        return "Citadel of " + factionName;
    }
    //-----------


    // IInventory Overrides for banner stack
    @Override
    public int getSizeInventory() {
        return NUM_SLOTS;
    }

    @Override
    public boolean isEmpty() {
        return super.isEmpty() && bannerStack.isEmpty();
    }

    @Override
    public ItemStack getStackInSlot(int index) {
        if (index == BANNER_SLOT_INDEX)
            return bannerStack;
        return super.getStackInSlot(index);
    }

    @Override
    public ItemStack decrStackSize(int index, int count) {
        if (index == BANNER_SLOT_INDEX) {
            int numToTake = Math.max(count, bannerStack.getCount());
            ItemStack result = bannerStack.copy();
            result.setCount(numToTake);
            bannerStack.setCount(bannerStack.getCount() - numToTake);
            return result;
        }
        return super.decrStackSize(index, count);
    }

    @Override
    public ItemStack removeStackFromSlot(int index) {
        if (index == BANNER_SLOT_INDEX) {
            ItemStack result = bannerStack;
            bannerStack = ItemStack.EMPTY;
            return result;
        }
        return super.removeStackFromSlot(index);
    }

    @Override
    public void setInventorySlotContents(int index, ItemStack stack) {
        if (index == BANNER_SLOT_INDEX) {
            bannerStack = stack;
			/*
			if(stack.getItem() instanceof ItemBanner)
			{
				int newColour = ItemBanner.getBaseColor(stack).getColorValue();
				if(!world.isRemote) 
				{
					Faction faction = WarForgeMod.INSTANCE.GetFaction(mFactionUUID);
					if(faction != null)
					{
						faction.mColour = newColour;
						mColour = newColour;
						markDirty();
					}
				}
			}
			*/
        } else
            super.setInventorySlotContents(index, stack);
    }

    @Override
    public boolean isItemValidForSlot(int index, ItemStack stack) {
        if (index == BANNER_SLOT_INDEX) {
            return stack.getItem() instanceof ItemBanner || stack.getItem() instanceof ItemShield;
        }
        return super.isItemValidForSlot(index, stack);
    }

    @Override
    public void clear() {
        super.clear();
        bannerStack = ItemStack.EMPTY;
    }

    @Override
    public void onServerSetFaction(Faction faction) {
        super.onServerSetFaction(faction);
        TileEntity te = world.getTileEntity(pos.up(2));
        if (te instanceof TileEntityDummy)
            ((TileEntityDummy) te).setLaserRender(true);
    }


    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setUniqueId("placer", placer);

        NBTTagCompound bannerStackTags = new NBTTagCompound();
        bannerStack.writeToNBT(bannerStackTags);
        nbt.setTag("banner", bannerStackTags);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        bannerStack = new ItemStack(nbt.getCompoundTag("banner"));
        placer = nbt.getUniqueId("placer");
    }
}
