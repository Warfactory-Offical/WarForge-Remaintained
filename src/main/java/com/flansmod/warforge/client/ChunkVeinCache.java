package com.flansmod.warforge.client;

import akka.japi.Pair;
import com.flansmod.warforge.api.Quality;
import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.VeinKey;
import com.flansmod.warforge.api.WarforgeCache;
import com.flansmod.warforge.common.DimChunkPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.flansmod.warforge.client.ClientProxy.VEIN_ENTRIES;

@SideOnly(Side.CLIENT)
public class ChunkVeinCache {
    protected WarforgeCache<DimChunkPos, Pair<Vein, Quality>> cache;

    @SideOnly(Side.CLIENT)
    public ChunkVeinCache() {
        cache = new WarforgeCache<>(0, 64);
    }

    @SideOnly(Side.CLIENT)
    public void purge(){
        cache.clear();
    }

    public void add(DimChunkPos chunkPos, int veinID, byte qualOrd){
        if (veinID == -1 || qualOrd == -1) {
            cache.put(chunkPos, null);
            return;
        }

        cache.put(chunkPos, new Pair<>(VEIN_ENTRIES.get(veinID), Quality.values()[qualOrd]));
    }

    public Pair<Vein, Quality> get(DimChunkPos chunkPos) {
        return cache.get(chunkPos);
    }

    public boolean contains(DimChunkPos chunkPosKey) {
        return cache.contains(chunkPosKey);
    }
}
