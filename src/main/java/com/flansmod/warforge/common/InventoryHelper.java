package com.flansmod.warforge.common;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Adds access to the InventoryPlayer stack combination methods for arbitrary inventories
 */
public class InventoryHelper
{
	public static boolean addItemStackToInventory(IInventory inventory, ItemStack stack, boolean isCreative)
	{
		if(stack == null || stack.isEmpty())
			return false;
		else if(stack.getCount() == 0)
			return false;
		else
		{
			try
			{
				int i;
				
				if(stack.isItemDamaged())
				{
					i = getFirstEmptyStack(inventory);
					
					if(i >= 0)
					{
						ItemStack stackToAdd = stack.copy();
						stackToAdd.setAnimationsToGo(5);
						inventory.setInventorySlotContents(i, stackToAdd);
						stack.setCount(0);
						return true;
					}
					else if(isCreative)
					{
						stack.setCount(0);
						return true;
					}
					return false;
				}
				else
				{
					do
					{
						i = stack.getCount();
						stack.setCount(storePartialItemStack(inventory, stack));
					}
					while(stack.getCount() > 0 && stack.getCount() < i);
					
					if(stack.getCount() == i && isCreative)
					{
						stack.setCount(0);
						return true;
					}
					else
					{
						return stack.getCount() < i;
					}
				}
			}
			catch(Throwable throwable)
			{
				return false;
			}
		}
	}
	
	public static int storeItemStack(IInventory inventory, ItemStack stack)
	{
		for(int i = 0; i < inventory.getSizeInventory(); ++i)
		{
			ItemStack oldStack = inventory.getStackInSlot(i);
			if(!oldStack.isEmpty() && oldStack.getItem() == stack.getItem() &&
					oldStack.isStackable() && oldStack.getCount() < oldStack.getMaxStackSize() &&
					oldStack.getCount() < inventory.getInventoryStackLimit() &&
					(!oldStack.getHasSubtypes() || oldStack.getItemDamage() == stack.getItemDamage())
					&& ItemStack.areItemStackTagsEqual(oldStack, stack))

			{
				return i;
			}
		}
		
		return -1;
	}
	
	public static int storePartialItemStack(IInventory inventory, ItemStack stack)
	{
		Item item = stack.getItem();
		int itemCount = stack.getCount();
		int emptySlot;
		
		//If the item doesn't stack, just find an empty slot for it
		if(stack.getMaxStackSize() == 1)
		{
			emptySlot = getFirstEmptyStack(inventory);
			//If it is impossible, return
			if(emptySlot < 0)
			{
				return itemCount;
			}
			else
			{
                ItemStack oldStack = inventory.getStackInSlot(emptySlot);
                if(oldStack.isEmpty())
				{
					inventory.setInventorySlotContents(emptySlot, stack.copy());
				}
				return 0;
			}
		}
		else
		{
			emptySlot = storeItemStack(inventory, stack);
			if(emptySlot < 0)
			{
				emptySlot = getFirstEmptyStack(inventory);
			}

            if (emptySlot >= 0) {
                ItemStack oldStack = inventory.getStackInSlot(emptySlot);

                if (oldStack.isEmpty()) {
                    oldStack = new ItemStack(item, 0, stack.getItemDamage());
                    if (stack.hasTagCompound())
                        oldStack.setTagCompound(stack.getTagCompound().copy());
                    inventory.setInventorySlotContents(emptySlot, oldStack);
                }

                int l = Math.min(inventory.getInventoryStackLimit() - oldStack.getCount(),
                        Math.min(itemCount, oldStack.getMaxStackSize() - oldStack.getCount()));

                if (l != 0) {
                    itemCount -= l;
                    oldStack.setCount(oldStack.getCount() + l);
                    oldStack.setAnimationsToGo(5);
                }
            }
            return itemCount;
        }
	}
	
	/**
	 * Method from InventoryPlayer
	 */
	public static int getFirstEmptyStack(IInventory inventory)
	{
		for(int i = 0; i < inventory.getSizeInventory(); ++i) {
            if(inventory.getStackInSlot(i).isEmpty())
                return i;
        }
		
		return -1;
	}
	
}
