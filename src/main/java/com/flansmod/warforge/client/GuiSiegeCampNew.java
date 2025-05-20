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
import net.minecraft.world.storage.MapData;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class GuiSiegeCampNew {

    public static ModularScreen makeGUI
            (DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks) {

        EntityPlayer player = Minecraft.getMinecraft().player;
        WorldClient world = (WorldClient) player.world;

        ChunkPos centerChunk = siegeCampPos.toChunkPos();
        int mapCenterX = (centerChunk.x << 4) + 8;
        int mapCenterZ = (centerChunk.z << 4) + 8;


        int radius = 2;  // 2 chunks in each direction → 5x5 total area
        int centerX = centerChunk.x;
        int centerZ = centerChunk.z;
        List<Thread> threads = new ArrayList<>();


        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                Chunk chunk = world.getChunkProvider().getLoadedChunk(x, z);
                if (chunk != null) {
                    int[] rawChunk = new int[16 * 16];
                    for (int chunkX = 0; chunkX < 16; chunkX++) {
                        for (int chunkZ = 0; chunkZ < 16; chunkZ++) {
                            int y = chunk.getHeightValue(chunkX, chunkZ);
                            MapColor blockcolor;
                            try {
                                Field blockcolorField = chunk.getBlockState(chunkX, y, chunkZ).getBlock().getClass().getDeclaredField("blockMapColor");
                                blockcolorField.setAccessible(true);
                                blockcolor = (MapColor) blockcolorField.get(chunk.getBlockState(chunkX, y, chunkZ).getBlock());
                            } catch (NoSuchFieldException | IllegalAccessException e) {
                                throw new RuntimeException(e);
                            }
                            int index = chunkX + chunkZ * 16;
                            rawChunk[index] = (0xFF << 24) | blockcolor.colorValue;
                        }
                    }
                    int[] heightMapCopy = chunk.getHeightMap().clone();
                    ChunkDynamicTextureThread thread = new ChunkDynamicTextureThread(4, "chunk" + x + "" + z, rawChunk, heightMapCopy);
                    threads.add(thread);
                    thread.run();
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

//        MapData data = new MapData("gui_preview_map");
//        data.scale = 0;
//        data.xCenter = mapCenterX;
//        data.zCenter = mapCenterZ;
//        data.dimension = world.provider.getDimension();
//        data.colors = new byte[128 * 128];
//        for (int px = 0; px < 128; px++) {
//            for (int pz = 0; pz < 128; pz++) {
//                int worldX = mapCenterX + (px - 64);
//                int worldZ = mapCenterZ + (pz - 64);
//
//                Chunk chunk = world.getChunkProvider().getLoadedChunk(worldX >> 4, worldZ >> 4);
//                if (chunk == null) continue;
//
//                int y = chunk.getHeightValue(worldX & 15, worldZ & 15);
//                BlockPos pos = new BlockPos(worldX, y-1, worldZ);
//                IBlockState state = world.getBlockState(pos);
//                MapColor mapColor = state.getMapColor(world, pos);
//
//                byte mapByte = (byte) mapColor.colorIndex;
//                data.colors[px + pz * 128] = mapByte;
//            }
//        }
//

        ModularPanel panel = ModularPanel.defaultPanel("citadel_upgrade_panel")
                .width(500)
                .height(500)
                .child(new MapDrawable("chunk11").asWidget().size(16*4, 16*4));

        return new ModularScreen(panel);
    }

}

