package com.flansmod.warforge.common.network;

import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.init.VeinConfigHandler;
import com.flansmod.warforge.common.WarForgeMod;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import java.util.ArrayList;

import static com.flansmod.warforge.client.ClientProxy.VEIN_ENTRIES;

public class PacketVeinEntries extends PacketBase {
    // clients ask for data, servers send data
    // called by the packet handler to convert to a byte stream to send
    private ArrayList<Vein> veinBuffer = new ArrayList<>();
    private int byteCount = 0;

    private static final int maxPacketByteCount = 1024;

    // fills the pass packet as much as possible from the veins array passed beginning at the start index passed
    // guarantees that at least one vein will be put into packet, returning the index of the first vein not included
    public int fillFrom(ArrayList<Vein> veins, int startIndex) {
        while (startIndex < veins.size() && tryAddVein(veins.get(startIndex))) { ++startIndex; }  // post increment could be used but intellij complains
        return startIndex;
    }

    // tries to fill up a packet to be at least 512 bytes
    public boolean tryAddVein(Vein veinToAdd) {
        if (byteCount > 0 && veinToAdd.SERIALIZED_ENTRY.readableBytes() + byteCount > maxPacketByteCount) { return false; }

        veinBuffer.add(veinToAdd);
        byteCount += veinToAdd.SERIALIZED_ENTRY.readableBytes();
        return true;
    }

    // we want this to be compressed
    @Override
    public boolean canUseCompression() { return true; }

    // called by the packet handler to convert to a byte stream to send
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        for (Vein vein : veinBuffer) {
            final int veinBufBytes = vein.SERIALIZED_ENTRY.readableBytes();
            data.writeBytes(vein.SERIALIZED_ENTRY, 0, veinBufBytes);
        }
    }

    // called by the packet handler to make the packet from a byte stream after construction
    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        // continue and try to deserialize the data until it has all been read through
        while (data.readableBytes() > 0) {
            veinBuffer.add(new Vein(data));
        }
    }

    // always called on packet after decodeInto has been called
    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        // server shouldn't get these
    }

    // always called on packet after decodeInto has been called
    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        // store the received veins
        for (Vein vein : veinBuffer) {
            VEIN_ENTRIES.put(vein.getId(), vein);
            WarForgeMod.LOGGER.atDebug().log("Received vein of id <" + vein.getId() + "> with data: \n" + vein);
        }
    }
}
