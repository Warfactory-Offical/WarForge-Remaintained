package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.server.Faction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemBanner;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldServer;

import java.util.UUID;

import static com.flansmod.warforge.common.blocks.BlockCitadel.FACING;
import static com.flansmod.warforge.common.blocks.BlockDummy.MODEL;

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
        rotation = 0;
        EnumFacing blockRotation = world.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())).getValue(FACING);
        switch (blockRotation) {
            case NORTH: //WORKING
                rotation = 270;
                break;
            case EAST: //WORKING
                rotation = 180;
                break;
            case SOUTH: //WORKING
                rotation = 90;
                break;
            case WEST: //WORKING
                rotation = 0;
        }

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
    public String getClaimDisplayName() { //FIXME
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

    /**
     * Builds the statue and assigns rotation.
     *
     * @param faction faction to build with.
     */
    @Override
    public void onServerSetFaction(Faction faction) {

        //To do: save rotation to NBT
        IBlockState state = world.getBlockState(pos);
        world.setBlockState(pos.up(), Content.statue.getDefaultState().withProperty(MODEL, BlockDummy.modelEnum.KNIGHT), 3);
        world.notifyBlockUpdate(pos.up(), state, state, 3);


        TileEntity teMiddle = world.getTileEntity(pos.up());
        if (teMiddle instanceof TileEntityDummy) {
            ((TileEntityDummy) teMiddle).setMaster(pos);
        }

        world.setBlockState(pos.up(2), Content.dummyTranslusent.getDefaultState().withProperty(MODEL, BlockDummy.modelEnum.TRANSLUCENT), 3);
        world.notifyBlockUpdate(pos.up(2), state, state, 3);

        TileEntity teTop = world.getTileEntity(pos.up(2));
        if (teTop instanceof TileEntityDummy) {
            ((TileEntityDummy) teMiddle).setMaster(pos);
        }

        super.onServerSetFaction(faction);
        TileEntity te = world.getTileEntity(pos.up(2));
        if (te instanceof TileEntityDummy)
            ((TileEntityDummy) te).setLaserRender(true);


    }


    public void onServerCreateFaction(Faction faction) {

        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0F, 1.0F);
        world.playSound(null, pos, SoundEvents.BLOCK_ANVIL_USE, SoundCategory.PLAYERS, 1.0F, 1.2F);
        ((WorldServer) world).spawnParticle(
                EnumParticleTypes.EXPLOSION_LARGE,
                pos.getX(), pos.getY(), pos.getZ(),
                32,
                0.0D, 2.5D, 0.0D,
                1.0D
        );
        onServerSetFaction(faction);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setUniqueId("placer", placer);
        nbt.setFloat("rotation", rotation);

        NBTTagCompound bannerStackTags = new NBTTagCompound();
        bannerStack.writeToNBT(bannerStackTags);
        nbt.setTag("banner", bannerStackTags);

        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);
        rotation = nbt.getFloat("rotation");

        bannerStack = new ItemStack(nbt.getCompoundTag("banner"));
        placer = nbt.getUniqueId("placer");
    }

}
