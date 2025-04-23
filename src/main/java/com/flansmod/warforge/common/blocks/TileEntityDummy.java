package com.flansmod.warforge.common.blocks;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityDummy extends TileEntity implements IBlockDummy {
    protected String playerFlag; //Owner's flag
    private BlockPos masterPos;
    private boolean canRenderLaser = false;
    private float[] laserRGB = new float[]{0F, 0F, 0F}; //pure white


    public void setMaster(BlockPos pos) {
        this.masterPos = pos;
        markDirty();
    }

    public boolean getLaserRender() {
        return canRenderLaser;
    }

    public void setLaserRender(boolean bool) {
        canRenderLaser = bool;
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
        markDirty();

    }

    public float[] getLaserRGB() {
        return laserRGB;
    }

    public void setLaserRGB(int colour) {
        float r = ((colour >> 16) & 0xFF) / 255.0f;
        float g = ((colour >> 8) & 0xFF) / 255.0f;
        float b = (colour & 0xFF) / 255.0f;
        this.laserRGB = new float[]{r,g,b};
        world.markBlockRangeForRenderUpdate(pos, pos);
        world.notifyBlockUpdate(pos, world.getBlockState(pos), world.getBlockState(pos), 3);
        world.scheduleBlockUpdate(pos, this.getBlockType(), 0, 0);
        markDirty();
    }

    public void setLaserRGB(float[] laserRGB) {
        this.laserRGB = laserRGB;
    }

    public BlockPos getMasterTile() {
        if (world == null || masterPos == null) return null;
        return masterPos;
    }//Primitive but sufficent for the time tbh

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        super.writeToNBT(compound);
        compound.setBoolean("laser", canRenderLaser);
        compound.setFloat("r", laserRGB[0]);
        compound.setFloat("g", laserRGB[1]);
        compound.setFloat("b", laserRGB[2]);
        if (masterPos != null) {
            compound.setLong("master", masterPos.toLong());
        }
        return compound;
    }


    @Override
    public void onLoad() {
        super.onLoad();
        if (world.isRemote) return;
        if (masterPos == null) {
            WarForgeMod.LOGGER.error("TileEntityDummy at " + this.pos + "had null Master pos, attempting to locate it");
            for (int i = 1; i <= 3; i++) {
                Block block = world.getBlockState(pos.down(i)).getBlock();
                if (block instanceof MultiBlockColumn) {
                    setMaster(pos.down(i));
                    WarForgeMod.LOGGER.info("TileEntityDummy at " + this.pos + " found master at " + pos.down(i));
                    markDirty();
                    return;
                }
                WarForgeMod.LOGGER.error("TileEntityDummy at " + this.pos + " cannot find it's master, this is a bug, report it to the developer!");
            }
        }

    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        super.readFromNBT(compound);
        canRenderLaser = compound.getBoolean("laser");
        laserRGB[0] = compound.getFloat("r");
        laserRGB[1] = compound.getFloat("g");
        laserRGB[2] = compound.getFloat("b");


        if (compound.hasKey("master")) {
            masterPos = BlockPos.fromLong(compound.getLong("master"));
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
        NBTTagCompound tags = super.getUpdateTag();
        tags.setBoolean("laser", canRenderLaser);
        tags.setFloat("r", laserRGB[0]);
        tags.setFloat("g", laserRGB[1]);
        tags.setFloat("b", laserRGB[2]);
        if(masterPos != null)
            tags.setLong("master", masterPos.toLong());


        return tags;
    }

    @Override
    public void handleUpdateTag(NBTTagCompound tags) {
        setLaserRender(tags.getBoolean("laser"));
        laserRGB[0] = tags.getFloat("r");
        laserRGB[1] = tags.getFloat("g");
        laserRGB[2] = tags.getFloat("b");
        masterPos = BlockPos.fromLong(tags.getLong("master"));


    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if(canRenderLaser) {
            BlockPos pos = getPos();
            return new AxisAlignedBB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 0.5, 256, pos.getZ() + 0.5);
        }
        return super.getRenderBoundingBox();
    }

}
