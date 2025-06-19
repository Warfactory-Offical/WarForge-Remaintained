package com.flansmod.warforge.common.network;

import akka.japi.Pair;
import com.flansmod.warforge.api.Quality;
import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import com.flansmod.warforge.api.WarforgeCache;
import com.flansmod.warforge.common.DimChunkPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.Siege;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.flansmod.warforge.client.ClientProxy.CHUNK_VEIN_CACHE;

//FIXME: this seems to completely break the fucking networking
public class PacketChunkPosVeinID extends PacketBase {
    public DimChunkPos veinLocation = null;
    public int resultID = -1;
    public byte resultQualOrd = -1;

    // clients ask for data, servers send data
    // called by the packet handler to convert to a byte stream to send
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        if (veinLocation == null) {
            WarForgeMod.LOGGER.atError().log("Premature ChunkPosVeinID packet with null vein location.");
            throw new NullPointerException("Premature ChunkPosVeinID Packet w/o vein location initialized");
        }

        // encode the chunk position
        data.writeInt(veinLocation.mDim);
        data.writeInt(veinLocation.x);
        data.writeInt(veinLocation.z);
        data.writeInt(resultID);
        data.writeByte(resultQualOrd);
    }

    // called by the packet handler to make the packet from a byte stream after construction
    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        try {
            veinLocation = new DimChunkPos(data.readInt(), data.readInt(), data.readInt());
            resultID = data.readInt();
            resultQualOrd = data.readByte();
        } catch (Exception exception) {
            WarForgeMod.LOGGER.atError().log("Received a faulty ChunkVeinPosPacket: " + data.toString());
            veinLocation = null;
            resultID = -1;
            resultQualOrd = -1;
        }
    }

    // always called on packet after decodeInto has been called
    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        if (veinLocation == null) {
            WarForgeMod.LOGGER.atError().log("Decoded ChunkPosVeinID packet without position; ignoring packet.");
            return;
        }

        // check if the player should even receive data about this chunk
        DimChunkPos playerPos = new DimChunkPos(playerEntity.dimension, playerEntity.getPosition());
        if (!Siege.isPlayerInRadius(veinLocation, playerPos)) {
            WarForgeMod.LOGGER.atError().log("Detected player outside <" + playerPos.toString() +
                    "> of queried chunk's <" + veinLocation.toString() + "> radius. Dropping packet");
            return;
        }

        // if the player is within a reasonable sqr radius (1) of the queried chunk, process and send data
        Pair<Vein, Quality> veinInfo = VeinKey.getVein(veinLocation, FMLCommonHandler.instance().getMinecraftServerInstance().worlds[0].getSeed());
        if (veinInfo != null) {
            resultID = veinInfo.first().getID();  // the client should have a copy of the vein to refer the ID w/
            resultQualOrd = (byte) veinInfo.second().ordinal();  // we need to tell them the quality
        }

        // if some error occurred or if a vein didn't exist, -1 for both as the default will be sent
        WarForgeMod.NETWORK.sendTo(this, playerEntity);  // 'encode into' handles getting of important data
    }

    // always called on packet after decodeInto has been called
    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
        CHUNK_VEIN_CACHE.add(veinLocation, resultID, resultQualOrd);  // we now have the necessary data about the vein
    }
}
