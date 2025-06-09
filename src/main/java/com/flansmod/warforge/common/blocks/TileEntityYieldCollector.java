package com.flansmod.warforge.common.blocks;

import java.util.*;
import java.util.Arrays;

import akka.japi.Pair;
import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import com.flansmod.warforge.common.InventoryHelper;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.ChunkPos;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import static com.flansmod.warforge.common.CommonProxy.YIELD_QUALITY_MULTIPLIER;
import static com.flansmod.warforge.common.WarForgeConfig.VEIN_MAP;

public abstract class TileEntityYieldCollector extends TileEntityClaim implements IInventory
{
	public static final int NUM_YIELD_STACKS = 9;
	public static final int NUM_BASE_SLOTS = NUM_YIELD_STACKS;
			
	protected abstract float getYieldMultiplier();

	// The yield stacks are where items arrive when your faction is above a deposit
	protected ItemStack[] yieldStacks = new ItemStack[NUM_YIELD_STACKS];

	public TileEntityYieldCollector()
	{
        Arrays.fill(yieldStacks, ItemStack.EMPTY);
	}
			
	public void processYield(int numYields) {
		if(world.isRemote) { return; }

		ChunkPos chunk = new ChunkPos(getPos());
		if (!VEIN_MAP.containsKey(world.provider.getDimension())) { return; }  // if dim doesn't have any veins

		// get vein data
		Pair<Vein, VeinKey.Quality> vein_info = VeinKey.getVein(world.provider.getDimension(), chunk.x, chunk.z, world.getSeed());
		if (vein_info == null) {
			// extra precaution in case something goes wrong
			WarForgeMod.LOGGER.atError().log("Unexpected null vein info. Terminating yield processing.");
			return;
		}
		Vein chunk_vein = vein_info.first();
		VeinKey.Quality vein_quality = vein_info.second();

		if (chunk_vein.isNullVein()) { return; }  // ignore null vein

		Random rand = new Random((WarForgeMod.currTickTimestamp * world.getSeed()) * 2654435761L);
		ArrayList<ItemStack> vein_components = new ArrayList<>(chunk_vein.component_ids.length);

		// for each component in the vein, attempt to yield it numYields many times
		for (int i = 0; i < chunk_vein.component_ids.length; ++i) {
			int num_items = 0;  // figure out how many items of this component are needed

			// determine yield amount based on quality and component base yield
			float curr_yield = chunk_vein.component_yields[i];

			// modify yield based on quality
			if (vein_quality == VeinKey.Quality.POOR) { curr_yield /= YIELD_QUALITY_MULTIPLIER; }
			else if (vein_quality == VeinKey.Quality.RICH) { curr_yield *= YIELD_QUALITY_MULTIPLIER; }

			// calculate the number of times to yield this component
			for (int j = 0; j < numYields; ++j) {
				if (rand.nextInt(1000) < chunk_vein.component_weights[i]) {
					num_items += (int) curr_yield;
					float remaining_yield = curr_yield - (float) num_items;

					// handle fractional yield cases
					if (remaining_yield != 0) {
						int yield_chance = (int) (remaining_yield * 1000);
						if (rand.nextInt(1000) < yield_chance) {
							num_items += 1;
						}
					}
				}
			}

			if (num_items == 0) { continue; } // adding an itemstack with 0 of the item will result in an air itemstack

			// attempt to locate the item and append the new item stack representing yield amounts
			final Item curr_component = ForgeRegistries.ITEMS.getValue(chunk_vein.component_ids[i]);
			if (curr_component == null) {
				// item does not exist for some reason
				WarForgeMod.LOGGER.atError().log("Got null item component for vein w/ key: " + chunk_vein.translation_key);
				continue;
			}

			vein_components.add(new ItemStack(curr_component, num_items));
		}

		// try to add the items
		for (ItemStack curr_component_stack : vein_components) {
			if(!InventoryHelper.addItemStackToInventory(this, curr_component_stack, false))
			{
				WarForgeMod.LOGGER.atError().log("Failed to add <" + curr_component_stack.toString() + "> to yield " +
						"collector at " + this.getPos());
			}
		}
		
		markDirty();
	}
	
	@Override
	public void onLoad()
	{
		if(!world.isRemote)
		{
			Faction faction = WarForgeMod.FACTIONS.getFaction(factionUUID);
			if(faction != null)
			{
				int pendingYields = faction.claims.get(this.getClaimPos());
				if(pendingYields > 0)
				{
					processYield(pendingYields);
				}
				faction.claims.replace(this.getClaimPos(), 0);
			}
			else if(!factionUUID.equals(Faction.nullUuid))
			{
				WarForgeMod.LOGGER.error("Loaded YieldCollector with invalid faction");
			}
		}
		
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt)
	{
		super.writeToNBT(nbt);
		
		// Write all our stacks out		
		for(int i = 0; i < NUM_YIELD_STACKS; i++)
		{
			NBTTagCompound yieldStackTags = new NBTTagCompound();
			yieldStacks[i].writeToNBT(yieldStackTags);
			nbt.setTag("yield_" + i, yieldStackTags);
		}
		
		return nbt;
	}

	
	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		super.readFromNBT(nbt);
	
		// Read inventory, or as much as we can find
		for(int i = 0; i < NUM_YIELD_STACKS; i++)
		{
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
	public boolean isEmpty() 
	{
		for(int i = 0; i < NUM_YIELD_STACKS; i++)
			if(!yieldStacks[i].isEmpty())
				return false;
		return true;
	}
	// In terms of indexing, the yield stacks are 0 - 8
	@Override
	public ItemStack getStackInSlot(int index) 
	{
		if(index < NUM_YIELD_STACKS)
			return yieldStacks[index];
		return ItemStack.EMPTY;
	}
	@Override
	public ItemStack decrStackSize(int index, int count) 
	{
		if(index < NUM_YIELD_STACKS)
		{
			int numToTake = Math.max(count, yieldStacks[index].getCount());
			ItemStack result = yieldStacks[index].copy();
			result.setCount(numToTake);
			yieldStacks[index].setCount(yieldStacks[index].getCount() - numToTake);
			return result;
		}
		return ItemStack.EMPTY;
	}
	@Override
	public ItemStack removeStackFromSlot(int index) 
	{
		ItemStack result = ItemStack.EMPTY;
		if(index < NUM_YIELD_STACKS)
		{
			result = yieldStacks[index];
			yieldStacks[index] = ItemStack.EMPTY;
		}
		return result;
	}
	@Override
	public void setInventorySlotContents(int index, ItemStack stack) 
	{
		if(index < NUM_YIELD_STACKS)
		{
			yieldStacks[index] = stack;
		}
	}
	@Override
	public int getInventoryStackLimit() 
	{
		return 64;
	}
	@Override
	public boolean isUsableByPlayer(EntityPlayer player) 
	{
		return factionUUID.equals(Faction.nullUuid) || WarForgeMod.FACTIONS.IsPlayerInFaction(player.getUniqueID(), factionUUID);
	}
	@Override
	public void openInventory(EntityPlayer player) { }
	@Override
	public void closeInventory(EntityPlayer player) { }
	@Override
	public boolean isItemValidForSlot(int index, ItemStack stack) 
	{
        return index < NUM_YIELD_STACKS;
    }
	@Override
	public int getField(int id)  { return 0; }
	@Override
	public void setField(int id, int value) { }
	@Override
	public int getFieldCount() { return 0; }
	@Override
	public void clear() 
	{
        Arrays.fill(yieldStacks, ItemStack.EMPTY);
	}
	// ----------------------------------------------------------
}
