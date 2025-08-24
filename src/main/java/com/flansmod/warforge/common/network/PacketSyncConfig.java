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

import java.util.HashMap;
import java.util.Map;

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
            WarForgeMod.LOGGER.error("Malformed config data NBT");
            return;
        }

        // Boolean flags
        WarForgeConfig.SIEGE_ENABLE_NEW_TIMER = compound.getBoolean("newSiegeTimer");
        WarForgeMod.LOGGER.info((WarForgeConfig.SIEGE_ENABLE_NEW_TIMER ? "Enabled " : "Disabled ")
                + "new siege timer system, per server requirements");

        WarForgeConfig.ENABLE_CITADEL_UPGRADES = compound.getBoolean("enableUpgrades");
        WarForgeMod.LOGGER.info((WarForgeConfig.ENABLE_CITADEL_UPGRADES ? "Enabled " : "Disabled ")
                + "citadel upgrade system, per server requirements");

        // Integers
        WarForgeConfig.SIEGE_MOMENTUM_MAX = (byte) compound.getInteger("maxMomentum");
        WarForgeConfig.SIEGE_MOMENTUM_DURATION = compound.getInteger("timeMomentum");

        // Long
        WarForgeConfig.SIEGE_BASE_TIME = compound.getLong("siegeTime");

        // Momentum map (stored as String → needs parsing back)
        String mapString = compound.getString("momentumMap");
        Map<Integer, Float> parsedMap = new HashMap<>();
        try {
            // The .toString() from Map looks like {1=0.9, 2=0.8, ...}
            // So we need to strip braces and split
            if (mapString.startsWith("{") && mapString.endsWith("}")) {
                mapString = mapString.substring(1, mapString.length() - 1);
            }
            for (String entry : mapString.split(",")) {
                String[] kv = entry.trim().split("=");
                if (kv.length == 2) {
                    int key = Integer.parseInt(kv[0].trim());
                    float val = Float.parseFloat(kv[1].trim());
                    parsedMap.put(key, val);
                }
            }
        } catch (Exception e) {
            WarForgeMod.LOGGER.error("Failed to parse momentumMap from config sync: " + mapString, e);
        }

        WarForgeConfig.YIELD_QUALITY_MULTIPLIER = compound.getFloat("yieldQualMult");

        WarForgeConfig.SIEGE_MOMENTUM_MULTI.clear();
        WarForgeConfig.SIEGE_MOMENTUM_MULTI.putAll(parsedMap);

        WarForgeMod.LOGGER.info("Synced siege config: baseTime=" + WarForgeConfig.SIEGE_BASE_TIME
                + ", maxMomentum=" + WarForgeConfig.SIEGE_MOMENTUM_MAX
                + ", duration=" + WarForgeConfig.SIEGE_MOMENTUM_DURATION
                + ", multipliers=" + WarForgeConfig.SIEGE_MOMENTUM_MULTI
                + ", yieldQualMult=" + WarForgeConfig.YIELD_QUALITY_MULTIPLIER);
    }

    public boolean canUseCompression() {return true;}
}
