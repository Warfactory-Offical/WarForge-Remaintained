package com.flansmod.warforge.client;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.flansmod.warforge.api.ChunkDynamicTextureThread;
import com.flansmod.warforge.api.MapDrawable;
import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.stream.IntStream;

public class GuiSiegeCampNew {

    @SideOnly(Side.CLIENT)
    public static ModularScreen makeGUI(DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks) {

        EntityPlayer player = Minecraft.getMinecraft().player;
        WorldClient world = (WorldClient) player.world;
        possibleAttacks.sort(Comparator
                .comparingInt((SiegeCampAttackInfo s) -> s.mOffset.getZ())
                .thenComparingInt(s -> s.mOffset.getX()));

        ChunkPos centerChunk = siegeCampPos.toChunkPos();


        int radius = 2;  // 2 chunks in each direction → 5x5 total area
        int centerX = centerChunk.x;
        int centerZ = centerChunk.z;
        List<Thread> threads = new ArrayList<>();

        boolean[][] adjesencyArray = new boolean[possibleAttacks.size()][4];
        threads.add(new Thread(() ->
                computeAdjacency(possibleAttacks, radius, adjesencyArray))
        );
        threads.get(0).start();
        Map<ChunkPos, Chunk> chunks = new LinkedHashMap<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk != null) chunks.put(chunk.getPos(), chunk);
            }
        }
        int[] minMax = chunks.values().parallelStream()
                .map(chunk -> {
                    int localMin = Integer.MAX_VALUE;
                    int localMax = Integer.MIN_VALUE;
                    for (int chunkX = 0; chunkX < 16; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 16; chunkZ++) {
                            int y = chunk.getHeightValue(chunkX, chunkZ) - 1;
                            if (y < localMin) localMin = y;
                            if (y > localMax) localMax = y;
                        }
                    }
                    return new int[]{localMin, localMax};
                }).reduce(new int[]{Integer.MAX_VALUE, Integer.MIN_VALUE}, (a, b) ->
                        new int[]{Math.min(a[0], b[0]), Math.max(a[1], b[1])
                        });

        int globalMin = minMax[0];
        int globalMax = minMax[1];
        int chunkID = 0;

        for (Chunk chunk : chunks.values()) {
            ChunkPos pos = chunk.getPos();
            int chunkX = pos.x;
            int chunkZ = pos.z;

            // 17×17 padded color + height
            //FIXME: fucked up the border I think, not major but can cause tiling artifacts
            int[] rawChunk17 = new int[17 * 17];
            int[] heightMap17 = new int[17 * 17];

            for (int dz = -1; dz <= 15; dz++) {
                for (int dx = -1; dx <= 15; dx++) {
                    int worldX = (chunkX << 4) + dx;
                    int worldZ = (chunkZ << 4) + dz;
                    int neighborCX = worldX >> 4;
                    int neighborCZ = worldZ >> 4;
                    int localX = worldX & 15;
                    int localZ = worldZ & 15;

                    Chunk neighbor = chunks.get(new ChunkPos(neighborCX, neighborCZ));
                    int index = (dx + 1) + (dz + 1) * 17;

                    if (neighbor != null) {
                        int y = neighbor.getHeightValue(localX, localZ);
                        heightMap17[index] = y;
                        IBlockState state = neighbor.getBlockState(localX, y - 1, localZ);
                        MapColor mapColor = state.getMapColor(world, new BlockPos(worldX, y - 1, worldZ));
                        rawChunk17[index] = 0xFF000000 | mapColor.colorValue;
                    } else {
                        int fallbackX = Math.min(Math.max(localX, 0), 15);
                        int fallbackZ = Math.min(Math.max(localZ, 0), 15);
                        int y = chunk.getHeightValue(fallbackX, fallbackZ);
                        heightMap17[index] = y;
                        IBlockState state = chunk.getBlockState(fallbackX, y - 1, fallbackZ);
                        MapColor mapColor = state.getMapColor(world, new BlockPos((chunkX << 4) + fallbackX, y - 1, (chunkZ << 4) + fallbackZ));
                        rawChunk17[index] = 0xFF000000 | mapColor.colorValue;
                    }
                }
            }

            ChunkDynamicTextureThread thread = new ChunkDynamicTextureThread(
                    4,
                    "chunk" + chunkID,
                    rawChunk17,
                    heightMap17,
                    globalMax,
                    globalMin
            );

            threads.add(thread);
            thread.start();
            chunkID++;
        }

        while (threads.stream().anyMatch(Thread::isAlive) || !ChunkDynamicTextureThread.queue.isEmpty()) {
            ChunkDynamicTextureThread.RegisterTextureAction textureAction = ChunkDynamicTextureThread.queue.poll();
            if (textureAction != null)
                textureAction.register();
        }
        int offset = 6;

        ModularPanel panel = ModularPanel.defaultPanel("citadel_upgrade_panel")
                .width((16 * 4) * 5 + (2 * offset))
                .height((16 * 4) * 5 + (2 * offset)
                );

        int id = 0;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                panel.child(new MapDrawable("chunk" + id, possibleAttacks.get(id), adjesencyArray[id]).asWidget().size(16 * 4).pos((i * (16 * 4) + offset), (j * (16 * 4) + offset)));
                id++;
            }
        }

        return new ModularScreen(panel);
    }

    public static void computeAdjacency(List<SiegeCampAttackInfo> list, int radius, boolean[][] retArr) {
        int size = 2 * radius + 1;
        int total = size * size;


        IntStream.range(0, total).parallel().forEach(i -> {
            SiegeCampAttackInfo current = list.get(i);
            UUID currentFaction = current.mFactionUUID;

            int x = i % size;
            int z = i / size;

            // North (z-1)
            if (z - 1 >= 0) {
                SiegeCampAttackInfo neighbor = list.get(i - size);
                retArr[i][0] = !currentFaction.equals(neighbor.mFactionUUID);
            }

            // East (x+1)
            if (x + 1 < size) {
                SiegeCampAttackInfo neighbor = list.get(i + 1);
                retArr[i][1] = !currentFaction.equals(neighbor.mFactionUUID);
            }

            // South (z+1)
            if (z + 1 < size) {
                SiegeCampAttackInfo neighbor = list.get(i + size);
                retArr[i][2] = !currentFaction.equals(neighbor.mFactionUUID);
            }

            // West (x-1)
            if (x - 1 >= 0) {
                SiegeCampAttackInfo neighbor = list.get(i - 1);
                retArr[i][3] = !currentFaction.equals(neighbor.mFactionUUID);
            }
        });

    }

}

