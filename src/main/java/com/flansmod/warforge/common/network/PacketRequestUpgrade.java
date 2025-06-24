package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import com.flansmod.warforge.server.Faction;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;

import java.util.UUID;

public class PacketRequestUpgrade extends PacketBase {
   public UUID factionID = Faction.nullUuid;


    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUUID(data, factionID);

    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        factionID = readUUID(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
                WarForgeMod.FACTIONS.requestLevelUp(playerEntity, factionID);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
