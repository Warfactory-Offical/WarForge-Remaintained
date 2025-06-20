package com.flansmod.warforge.client;

import com.flansmod.warforge.api.WarforgeCache;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketRequestNamePlate;
import lombok.RequiredArgsConstructor;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

public class PlayerNametagCache {
    protected WarforgeCache<String, NamePlateData> cache;

    @SideOnly(Side.CLIENT)
    public PlayerNametagCache(long l, int i) {
        cache = new WarforgeCache<>(l, i);
    }
    @SideOnly(Side.CLIENT)
    public void purge(){
        cache.clear();
    }
    public void add(String name, String faction, int color){

        cache.put(name, faction.isEmpty() ? null : new NamePlateData(faction,color));
    }
    public void remove(String name){
        cache.remove(name);
    }

    public @Nullable NamePlateData requestIfAbsent(String player){
        NamePlateData data = cache.get(player);
        if (data == null) {
            WarForgeMod.LOGGER.info("Requesting faction nametag for " + player);
            var packet = new PacketRequestNamePlate();
            packet.name = player;
            WarForgeMod.NETWORK.sendToServer(packet);
            cache.put(player, AWAITING_DATA);
            return null;
        }
        if (data == AWAITING_DATA) {
            return null; // already requested, still waiting
        }
        return data; // valid result
    }
    private static final NamePlateData AWAITING_DATA = new NamePlateData("<awaiting>", 0xFFFFFF);

    @RequiredArgsConstructor
   public static class NamePlateData {
       final public String name;
       final public int color;

   }

}
