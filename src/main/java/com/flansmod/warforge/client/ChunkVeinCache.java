package com.flansmod.warforge.client;

import com.flansmod.warforge.api.Vein;
import com.flansmod.warforge.api.WarforgeCache;
import com.flansmod.warforge.common.DimChunkPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import static com.flansmod.warforge.client.ClientProxy.VEIN_ENTRIES;

@SideOnly(Side.CLIENT)
public class ChunkVeinCache {
    protected WarforgeCache<DimChunkPos, Vein> cache;

    @SideOnly(Side.CLIENT)
    public ChunkVeinCache() {
        cache = new WarforgeCache<>(0, 64);
    }

    @SideOnly(Side.CLIENT)
    public void purge(){
        cache.clear();
    }

    public void add(DimChunkPos chunkPos, int veinID){
        cache.put(chunkPos, VEIN_ENTRIES.get(veinID));
    }
}
