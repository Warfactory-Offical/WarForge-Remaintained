package com.flansmod.warforge.common.network;

import akka.japi.Pair;
import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import com.flansmod.warforge.common.DimChunkPos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketChunkPosVeinID extends PacketBase {
    public DimChunkPos veinLocation = null;
    public int resultID = -1;
    public int resultQualOrd = -1;

    // clients ask for data, servers send data
    // called by the packet handler to convert to a byte stream to send
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        // encode the chunk position
        data.writeInt(veinLocation.mDim);
        data.writeInt(veinLocation.x);
        data.writeInt(veinLocation.z);

        // encode the vein index on the server
        if (!Minecraft.getMinecraft().world.isRemote) {
            Pair<Vein, VeinKey.Quality> veinInfo = VeinKey.getVein(veinLocation, Minecraft.getMinecraft().world.getSeed());
            data.writeInt(veinInfo.first().getID());  // the client should have a copy of the vein to refer the ID w/
            data.writeByte((byte) veinInfo.second().ordinal());  // we need to tell them the quality
        }
    }

    // called by the packet handler to make the packet from a byte stream after construction
    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        veinLocation = new DimChunkPos(data.readInt(), data.readInt(), data.readInt());

        // receive the ID on the client
        if (Minecraft.getMinecraft().world.isRemote) {
            resultID = data.readInt();
            resultQualOrd = data.readByte();
        }
    }

    // always called on packet after decodeInto has been called
    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    // always called on packet after decodeInto has been called
    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
