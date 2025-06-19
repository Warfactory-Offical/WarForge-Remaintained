package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.Vec3i;

public class PacketStartSiege extends PacketBase {
    public DimBlockPos mSiegeCampPos;
    public Vec3i mOffset;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeInt(mSiegeCampPos.dim);
        data.writeInt(mSiegeCampPos.getX());
        data.writeInt(mSiegeCampPos.getY());
        data.writeInt(mSiegeCampPos.getZ());

        data.writeByte(mOffset.getX());
        data.writeByte(mOffset.getZ());
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        int dim = data.readInt();
        int x = data.readInt();
        int y = data.readInt();
        int z = data.readInt();
        mSiegeCampPos = new DimBlockPos(dim, x, y, z);
        byte dx = data.readByte();
        byte dz = data.readByte();
        mOffset = new Vec3i(dx, 0, dz);

    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        WarForgeMod.FACTIONS.requestStartSiege(playerEntity, mSiegeCampPos, mOffset);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        WarForgeMod.LOGGER.error("Received start siege packet client side");
    }

}
