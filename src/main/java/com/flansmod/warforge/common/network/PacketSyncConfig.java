package com.flansmod.warforge.common.network;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

public class PacketSyncConfig extends  PacketBase {
    public String configNBT;
    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUTF(data,configNBT);
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        configNBT = readUTF(data);
    }

    @Override
    public void handleServerSide(EntityPlayerMP playerEntity) {
        //noop
    }

    @Override
    public void handleClientSide(EntityPlayer clientPlayer) {
        NBTTagCompound compound;
        try {
            compound = JsonToNBT.getTagFromJson(configNBT);
        } catch (NBTException e) {
            WarForgeMod.LOGGER.error("Malformed config data NBT ");
            return;
        }
        WarForgeConfig.SIEGE_ENABLE_NEW_TIMER = compound.getBoolean("newSiegeTimer");
        WarForgeMod.LOGGER.info((WarForgeConfig.SIEGE_ENABLE_NEW_TIMER ? "Enabled" : "Disabled") + "new siege timer system, per server requirements");
        WarForgeConfig.ENABLE_CITADEL_UPGRADES = compound.getBoolean("enableUpgrades");
        WarForgeMod.LOGGER.info((WarForgeConfig.ENABLE_CITADEL_UPGRADES ? "Enabled" : "Disabled") + "citadel upgrade system, per server requirements");
    }

    public boolean canUseCompression() {return true;}
}
