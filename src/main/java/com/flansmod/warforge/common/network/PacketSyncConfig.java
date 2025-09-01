package com.flansmod.warforge.common.network;

import com.flansmod.warforge.client.ClientProxy;
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

public class PacketSyncConfig extends PacketBase {
    public String configNBT;

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf data) {
        writeUTF(data, configNBT);
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


        // Momentum map (stored as String → needs parsing back)
        String mapString = compound.getString("momentumMap");
        Map<Byte, Integer> parsedMap = new HashMap<>();
        try {
            if (mapString.startsWith("{") && mapString.endsWith("}")) {
                mapString = mapString.substring(1, mapString.length() - 1);
            }
            for (String entry : mapString.split(",")) {
                String[] kv = entry.trim().split("=");
                if (kv.length == 2) {
                    byte key = Byte.parseByte(kv[0].trim());
                    int seconds = Integer.parseInt(kv[1].trim());
                    parsedMap.put(key, seconds);
                }
            }
        } catch (Exception e) {
            WarForgeMod.LOGGER.error("Failed to parse momentumMap from config sync: " + mapString, e);
        }

        WarForgeConfig.POOR_QUAL_MULT = compound.getFloat("poorQualMult");
        WarForgeConfig.FAIR_QUAL_MULT = compound.getFloat("fairQualMult");
        WarForgeConfig.RICH_QUAL_MULT = compound.getFloat("richQualMult");

        ClientProxy.megachunkLength = compound.getShort("megachunkLength");
        WarForgeConfig.SIEGE_ATTACKER_RADIUS = compound.getInteger("atkSiegeRadius");
        WarForgeConfig.SIEGE_DEFENDER_RADIUS = compound.getInteger("defSiegeRadius");

        WarForgeConfig.SIEGE_MOMENTUM_TIME.clear();
        WarForgeConfig.SIEGE_MOMENTUM_TIME.putAll(parsedMap);

        WarForgeMod.LOGGER.info("Synced siege config:"
                + " maxMomentum=" + WarForgeConfig.SIEGE_MOMENTUM_MAX
                + ", duration=" + WarForgeConfig.SIEGE_MOMENTUM_DURATION
                + ", multipliers=" + WarForgeConfig.SIEGE_MOMENTUM_TIME);
    }

    public boolean canUseCompression() {
        return true;
    }
}
