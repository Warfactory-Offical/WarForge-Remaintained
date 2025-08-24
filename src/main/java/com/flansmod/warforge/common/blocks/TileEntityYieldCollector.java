package com.flansmod.warforge.common.blocks;

import akka.japi.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinUtils;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.util.DimChunkPos;
import com.flansmod.warforge.common.util.InventoryHelper;
import com.flansmod.warforge.server.Faction;
import com.flansmod.warforge.server.StackComparable;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;

public abstract class TileEntityYieldCollector extends TileEntityClaim implements IInventory
{
	public static final int NUM_YIELD_STACKS = 9;
	public static final int NUM_BASE_SLOTS = NUM_YIELD_STACKS;

	protected abstract float getYieldMultiplier();

	// The yield stacks are where items arrive when your faction is above a deposit
	protected ItemStack[] yieldStacks = new ItemStack[NUM_YIELD_STACKS];

	public TileEntityYieldCollector() {
        Arrays.fill(yieldStacks, ItemStack.EMPTY);
	}

	public void processYield(HashMap<DimBlockPos, Integer> claims) {
		if(world.isRemote) { return; }  // we don't process on the client
		if (VEIN_HANDLER == null || !VEIN_HANDLER.hasFinishedInit) { return; }

		// check the number of yields to award and handle appropriately
		int numYields = claims.get(getClaimPos());
		if (numYields == 0) { return; }
		claims.replace(this.getClaimPos(), 0);

		DimChunkPos currPos = new DimChunkPos(world.provider.getDimension(), getPos());
		if (!VEIN_HANDLER.dimHasVeins(currPos.dim)) { return; }

		// get vein data
		Pair<Vein, Quality> veinInfo = VEIN_HANDLER.getVein(world.provider.getDimension(), currPos.x, currPos.z, world.getSeed());
		if (veinInfo == null) { return; }  // ignore null veins

		Vein currVein = veinInfo.first();
		Quality currQual = veinInfo.second();

		Random rand = new Random((WarForgeMod.currTickTimestamp * world.getSeed()) * 2654435761L);
		ArrayList<ItemStack> yieldComps = new ArrayList<>(currVein.compIds.size());

		// for each component in the vein, attempt to yield it numYields many times
		for (StackComparable currComp : currVein.compIds) {
			int numItems = 0;  // figure out how many items of this component are needed

			// determine yield amount based on quality and component base yield
			ArrayList<short[]> subCompYieldInfos = VeinUtils.getYieldInfo(currComp, veinInfo, currPos.dim);

			// yield this component the appropriate number of times
			for (int j = 0; j < numYields; ++j) {
				// for each subcomponent, apply the yielding logic
				for (short[] subCompInfo : subCompYieldInfos) {
					// apply a chance to yield this subcomponent
					if (rand.nextInt(VeinUtils.WEIGHT_FRACTION_TENS_POW) + 1 > subCompInfo[0]) { continue; }

					// apply a chance to yield an extra item of this sub component
					numItems += subCompInfo[1];  // add guaranteed yield
					if (rand.nextInt(VeinUtils.WEIGHT_FRACTION_TENS_POW) + 1 > subCompInfo[2]) { continue; }

					++numItems;  // add extra yield
				}
			}

			if (numItems == 0) { continue; } // adding an itemstack with 0 of the item will result in an air itemstack

			// attempt to locate the item and append the new item stack representing yield amounts
			ItemStack compStack = currComp.toItem(numItems);
			if (compStack == null) {
				// item does not exist for some reason
				WarForgeMod.LOGGER.atError().log("Got null item component for vein w/ key: " + currVein.translationKey);
				continue;
			}

			yieldComps.add(compStack);
		}

		if (yieldComps.size() == 0) { return; }  // don't go through the process of marking dirty if we won't do anything

		// try to add the items (THIS WILL CONSUME THE ITEMSTACKS, SO MAKE SURE THEY ARE COPIES)
		for (ItemStack currCompStack : yieldComps) {
			if(!InventoryHelper.addItemStackToInventory(this, currCompStack, false)) {
				WarForgeMod.LOGGER.atError().log("Failed to add <" + currCompStack.toString() + "> to yield " +
						"collector at " + this.getPos());
			}
		}

		markDirty();
	}

	@Override
	public void onLoad() {
		if(!world.isRemote) {
			Faction faction = WarForgeMod.FACTIONS.getFaction(factionUUID);
			if(faction != null) {
				processYield(faction.claims);
			} else if(!factionUUID.equals(Faction.nullUuid)) {
				WarForgeMod.LOGGER.error("Loaded YieldCollector with invalid faction");
			}
		}

	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		// Write all our stacks out
		for(int i = 0; i < NUM_YIELD_STACKS; i++) {
			NBTTagCompound yieldStackTags = new NBTTagCompound();
			yieldStacks[i].writeToNBT(yieldStackTags);
			nbt.setTag("yield_" + i, yieldStackTags);
		}

		return nbt;
	}


	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		// Read inventory, or as much as we can find
		for(int i = 0; i < NUM_YIELD_STACKS; i++) {
			if(nbt.hasKey("yield_" + i))
				yieldStacks[i] = new ItemStack(nbt.getCompoundTag("yield_" + i));
			else
				yieldStacks[i] = ItemStack.EMPTY;
		}
	}

	// ----------------------------------------------------------
	// The GIGANTIC amount of IInventory methods...
	@Override
	public String getName() { return factionName; }

	@Override
	public boolean hasCustomName() { return false; }

	@Override
	public int getSizeInventory() { return NUM_BASE_SLOTS; }

	@Override
	public boolean isEmpty() {
		for(int i = 0; i < NUM_YIELD_STACKS; i++)
			if(!yieldStacks[i].isEmpty())
				return false;
		return true;
	}

	// In terms of indexing, the yield stacks are 0 - 8
	@Override
	public ItemStack getStackInSlot(int index) {
		if(index < NUM_YIELD_STACKS)
			return yieldStacks[index];
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack decrStackSize(int index, int count) {
		if(index < NUM_YIELD_STACKS) {
			int numToTake = Math.max(count, yieldStacks[index].getCount());
			ItemStack result = yieldStacks[index].copy();
			result.setCount(numToTake);
			yieldStacks[index].setCount(yieldStacks[index].getCount() - numToTake);
			return result;
		}
		return ItemStack.EMPTY;
	}

	@Override
	public ItemStack removeStackFromSlot(int index) {
		ItemStack result = ItemStack.EMPTY;
		if(index < NUM_YIELD_STACKS) {
			result = yieldStacks[index];
			yieldStacks[index] = ItemStack.EMPTY;
		}
		return result;
	}

	@Override
	public void setInventorySlotContents(int index, ItemStack stack) {
		if(index < NUM_YIELD_STACKS) {
			yieldStacks[index] = stack;
		}
	}

	@Override
	public int getInventoryStackLimit() {
		return 64;
	}

	@Override
	public boolean isUsableByPlayer(EntityPlayer player) {
		return factionUUID.equals(Faction.nullUuid) || WarForgeMod.FACTIONS.IsPlayerInFaction(player.getUniqueID(), factionUUID);
	}

	@Override
	public void openInventory(EntityPlayer player) { }

	@Override
	public void closeInventory(EntityPlayer player) { }

	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) {
        return index < NUM_YIELD_STACKS;
    }

	@Override
	public int getField(int id)  { return 0; }

	@Override
	public void setField(int id, int value) { }

	@Override
	public int getFieldCount() { return 0; }

	@Override
	public void clear() {
        Arrays.fill(yieldStacks, ItemStack.EMPTY);
	}
	// ----------------------------------------------------------
}
