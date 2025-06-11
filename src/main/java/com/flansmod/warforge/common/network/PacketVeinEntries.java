package com.flansmod.warforge.common.network;

import com.flansmod.warforge.api.Vein;
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
    int startID = -1;
    private ArrayList<String> orderedEntryList = new ArrayList<>();

    // tries to fill up a packet to be at least 512 bytes
    public PacketVeinEntries fillFrom(ArrayList<Vein> orderedVeins, int index) {
        startID = index;
        int packetByteCount = 4;  // account for start id being added to packet
        while (packetByteCount < 512 && index < orderedVeins.size()) {
            packetByteCount += orderedVeins.get(index).VEIN_ENTRY.length();  // estimate of expected size in bytes

            orderedEntryList.add(orderedVeins.get(index++).VEIN_ENTRY);
        }

        return this;
    }

    public int entryCount() { return orderedEntryList.size(); }

    // we want this to be compressed
    @Override
    public boolean canUseCompression() { return true; }

    // called by the packet handler to convert to a byte stream to send
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        // only the server needs to encode the vein data to send, but regardless the startID should not be -1
        if (startID != -1) {
            data.writeInt(startID);
            // for each entry we can fit, place it in order
            for (String entry : orderedEntryList) {
                PacketBase.writeUTF(data, entry);
            }
        }
    }

    // called by the packet handler to make the packet from a byte stream after construction
    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        // only the client should receive, but we want to read ints, then UTF's, etc until the byte is emptied
        // if the readable byte count is <= 4, then there is barely enough room for an int, so error
        if (data.readableBytes() < 5) {
            WarForgeMod.LOGGER.atError().log("Vein entry packet received with too few bytes: " + data);
            return;
        }

        startID = data.readInt();  // first in is the start id

        while (data.readableBytes() > 0) {
            orderedEntryList.add(PacketBase.readUTF(data));
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
        for (int idOffset = 0; idOffset < orderedEntryList.size(); ++idOffset) {
            VEIN_ENTRIES.put(startID + idOffset, new Vein(orderedEntryList.get(idOffset), startID + idOffset));
            WarForgeMod.LOGGER.atDebug().log("Received vein entry of <" + orderedEntryList.get(idOffset) + "> w/ id: " + (startID + idOffset));
        }
    }
}
