package com.flansmod.warforge.common.network;

import akka.japi.Pair;
import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import com.flansmod.warforge.common.DimChunkPos;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketVeinInfo extends PacketBase {
    final public DimChunkPos veinLocation;  // we need to guarantee this data is here

    PacketVeinInfo(DimChunkPos chunkPos) {
        veinLocation = chunkPos;
    }

    // clients ask for data, servers send data
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
        }
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {

    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
