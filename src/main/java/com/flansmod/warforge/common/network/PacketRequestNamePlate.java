package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import lombok.AllArgsConstructor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

public class PacketRequestNamePlate extends PacketBase{
    public String name;
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUTF(data, name);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        name = readUTF(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        WarForgeMod.FACTIONS.requestNamePlateCacheEntry(playerEntity, name);
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {

    }
}
