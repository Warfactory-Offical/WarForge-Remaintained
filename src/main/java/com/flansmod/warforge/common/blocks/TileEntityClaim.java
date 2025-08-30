package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Faction;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.UUID;

import static com.flansmod.warforge.common.blocks.BlockCitadel.FACING;

public abstract class TileEntityClaim extends TileEntity implements IClaim {
    public int colour = 0xFF_FF_FF;
    public String factionName = "";
    protected UUID factionUUID = Faction.nullUuid;
    public byte rotation;

    // This is so weird
    private World worldCreate;

    // IClaim
    @Override
    public UUID getFaction() {
        return factionUUID;
    }

    public void increaseRotation() {
        rotation += 1;
        rotation  = (byte) (rotation % 8);
        updateTileEntity();
    }

    //TODO: This needs to be handled with enums and not array indexes
    public void onPlacedBy(EntityLivingBase placer) {
        // This locks in the placer as the only person who can create a faction using the interface on this citadel
        rotation = 0;
        EnumFacing blockRotation = world.getBlockState(new BlockPos(pos.getX(), pos.getY(), pos.getZ())).getValue(FACING);
        switch (blockRotation) {
            case NORTH: //WORKING
                rotation = 0;
                break;
            case EAST: //WORKING
                rotation = 6;
                break;
            case SOUTH: //WORKING
                rotation = 4;
                break;
            case WEST: //WORKING
                rotation = 2;
        }

    }



    @Override
    public void updateColour(int colour) {
        this.colour = colour;
        TileEntity te = world.getTileEntity(pos.up(2));
        if (te instanceof TileEntityDummy) {
            ((TileEntityDummy) te).setLaserRGB(colour);
        }
        updateTileEntity();
    }

    @Override
    public int getColour() {
        return colour;
    }

    @Override
    public TileEntity getAsTileEntity() {
        return this;
    }

    @Override
    public DimBlockPos getClaimPos() {
        if (world == null) {
            if (worldCreate == null)
                return DimBlockPos.ZERO;
            else
                return new DimBlockPos(worldCreate.provider.getDimension(), getPos());
        }
        return new DimBlockPos(world.provider.getDimension(), getPos());
    }

    @Override
    public boolean canBeSieged() {
        return true;
    }

    @Override
    public String getClaimDisplayName() {
        return factionName;
    }

    @Override
    public void onServerSetFaction(Faction faction) {
        if (faction == null) {
            factionUUID = Faction.nullUuid;
        } else {
            factionUUID = faction.uuid;
            updateColour(faction.colour);
            factionName = faction.name;
        }

        if(world.getBlockState(this.pos).getBlock() instanceof MultiBlockColumn) {
            MultiBlockColumn b = (MultiBlockColumn) world.getBlockState(this.pos).getBlock();
            b.setUpMultiblock(world, pos, b.getDefaultState());
        }

        updateTileEntity();
    }

    private void updateTileEntity() {
        if(world.isRemote) return;
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
        markDirty();
    }

    @Override
    public void setWorldCreate(World world) {
        worldCreate = world;
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        super.writeToNBT(nbt);

        nbt.setUniqueId("faction", factionUUID);
        nbt.setFloat("rotation", rotation);


        return nbt;
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        super.readFromNBT(nbt);

        factionUUID = nbt.getUniqueId("faction");
        rotation = nbt.getByte("rotation");

        // Verifications
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            Faction faction = WarForgeMod.FACTIONS.getFaction(factionUUID);
            if (!factionUUID.equals(Faction.nullUuid) && faction == null) {
                WarForgeMod.LOGGER.error("Faction " + factionUUID + " could not be found for citadel at " + pos);
                //world.setBlockState(getPos(), Blocks.AIR.getDefaultState());
            }
            if (faction != null) {
                colour = faction.colour;
                factionName = faction.name;
            }
        } else {
            WarForgeMod.LOGGER.error("Loaded TileEntity from NBT on client?");
        }

    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(getPos(), getBlockMetadata(), getUpdateTag());
    }

    @Override
    public void onDataPacket(net.minecraft.network.NetworkManager net, SPacketUpdateTileEntity packet) {
        NBTTagCompound tags = packet.getNbtCompound();

        handleUpdateTag(tags);
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        // You have to get parent tags so that x, y, z are added.
        NBTTagCompound tags = super.getUpdateTag();

        // Custom partial nbt write method
        tags.setUniqueId("faction", factionUUID);
        tags.setInteger("colour", colour);
        tags.setString("name", factionName);
        tags.setByte("rotation", rotation);
        return tags;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tags) {
        factionUUID = tags.getUniqueId("faction");
        colour = tags.getInteger("colour");
        factionName = tags.getString("name");
        rotation = tags.getByte("rotation");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox() {
        BlockPos pos = this.getPos();
        return new AxisAlignedBB(pos.add(-1, 0, -1), pos.add(2, 16, 2));

    }
}
