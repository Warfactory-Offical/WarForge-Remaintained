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
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiFunction;
import java.util.function.Function;

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

        int chunkID = 0;
        int max = 256;
        int min = 0;
        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk != null) {
                    int[] rawChunk = new int[16 * 16];
                    for (int chunkX = 0; chunkX < 16; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 16; chunkZ++) {
                            int y = chunk.getHeightValue(chunkX, chunkZ);
                            if (y < min) min = y;
                            if (y > max) max = y;
                            BlockPos blockPos = new BlockPos((x << 4) | chunkX, y - 1, (z << 4) | chunkZ);
                            IBlockState state = chunk.getBlockState(chunkX, y - 1, chunkZ);
                            MapColor blockcolor = state.getMapColor(world, blockPos);
                            int index = chunkX + chunkZ * 16;
                            rawChunk[index] = (0xFF << 24) | blockcolor.colorValue;
                        }
                    }
                    int[] heightMapCopy = chunk.getHeightMap().clone();
                    ChunkDynamicTextureThread thread = new ChunkDynamicTextureThread(4, "chunk" + chunkID, rawChunk, heightMapCopy, max, min);
                    threads.add(thread);
                    thread.start();
                    chunkID++;
                }
            }
        }
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

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

