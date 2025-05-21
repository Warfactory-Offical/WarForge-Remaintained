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
import scala.Int;

import java.util.ArrayList;
import java.util.List;

public class GuiSiegeCampNew {

    @SideOnly(Side.CLIENT)
    public static ModularScreen makeGUI(DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks) {

        EntityPlayer player = Minecraft.getMinecraft().player;
        WorldClient world = (WorldClient) player.world;

        ChunkPos centerChunk = siegeCampPos.toChunkPos();


        int radius = 2;  // 2 chunks in each direction → 5x5 total area
        int centerX = centerChunk.x;
        int centerZ = centerChunk.z;
        List<Thread> threads = new ArrayList<>();
        List<Chunk> chunks = new ArrayList<>();
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk != null) chunks.add(chunk);
            }
        }
        int[] minMax = chunks.parallelStream()
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
        int globalMax =  minMax[1];
        int chunkID = 0;
        for (Chunk chunk : chunks) {
            int chunkX = chunk.x;
            int chunkZ = chunk.z;
            int[] rawChunk = new int[16 * 16];
            for (int localX = 0; localX < 16; localX++) {
                for (int localZ = 0; localZ < 16; localZ++) {
                    int y = chunk.getHeightValue(localX, localZ);
                    BlockPos blockPos = new BlockPos((chunkX << 4) | localX, y - 1, (chunkZ << 4) | localZ);
                    IBlockState state = chunk.getBlockState(localX, y - 1, localZ);
                    MapColor blockcolor = state.getMapColor(world, blockPos);
                    int index = localX + localZ * 16;
                    rawChunk[index] = (0xFF << 24) | blockcolor.colorValue;
                }
            }
            int[] heightMapCopy = chunk.getHeightMap().clone();

            ChunkDynamicTextureThread thread = new ChunkDynamicTextureThread(4, "chunk" + chunkID, rawChunk, heightMapCopy, globalMax, globalMin);
            threads.add(thread);
            thread.start();
            chunkID++;
        }

        while (threads.stream().anyMatch(Thread::isAlive) || !ChunkDynamicTextureThread.queue.isEmpty()) {
            ChunkDynamicTextureThread.RegisterTextureAction textureAction = ChunkDynamicTextureThread.queue.poll();
            if (textureAction != null)
                textureAction.register();
        }

        ModularPanel panel = ModularPanel.defaultPanel("citadel_upgrade_panel").width(500).height(500);

        int id = 0;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                panel.child(new MapDrawable("chunk" + id).asWidget().size(16 * 4).pos((i * (16 * 4)), (j * (16 * 4))));
                id++;
            }
        }

        return new ModularScreen(panel);
    }

}

