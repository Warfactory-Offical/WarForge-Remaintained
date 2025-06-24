package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeMod;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class PacketNamePlateChange extends PacketBase {
    public boolean isRemove = false;
    public String faction = "";
    public String name = "";
    public int color;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        data.writeBoolean(isRemove);
        writeUTF(data, faction);
        writeUTF(data, name);
        data.writeInt(color);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        isRemove = data.readBoolean();
        faction = readUTF(data);
        name = readUTF(data);
        color = data.readInt();

    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {

    }

    @Override
    @SideOnly(Side.CLIENT)
    public void handleClientSide(EntityPlayer clientPlayer) {
        WarForgeMod.LOGGER.info("Recieved faction nametag for " + name + " [" + faction + "]");
        if (!isRemove)
            WarForgeMod.NAMETAG_CACHE.add(name, faction, color);
        else
            WarForgeMod.NAMETAG_CACHE.remove(name);


    }
}
