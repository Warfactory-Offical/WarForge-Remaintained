package com.flansmod.warforge.client;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.IngredientDrawable;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.flansmod.warforge.api.ChunkDynamicTextureThread;
import com.flansmod.warforge.api.Color4i;
import com.flansmod.warforge.api.modularui.MapDrawable;
import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.network.PacketStartSiege;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import com.flansmod.warforge.common.network.SiegeCampAttackInfoRender;
import com.flansmod.warforge.common.util.DimBlockPos;
import com.flansmod.warforge.server.StackComparable;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.biome.BiomeColorHelper;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.*;
import java.util.stream.IntStream;

public class GuiSiegeCamp {

    @SideOnly(Side.CLIENT)
    public static ModularScreen makeGUI(DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks, byte momentum) {

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
                        Material material = state.getMaterial();
                        Block block = state.getBlock();
                        BlockPos colorPos = new BlockPos((chunkX << 4) + fallbackX, y - 1, (chunkZ << 4) + fallbackZ);

                        Color4i mapColor;
                        if (block == Blocks.GRASS || block == Blocks.TALLGRASS || block == Blocks.DOUBLE_PLANT || material == Material.LEAVES) {
                            mapColor = Color4i.fromRGB(BiomeColorHelper.getFoliageColorAtPos(world, colorPos));
                        } else if (material == Material.WATER) {
                            mapColor = Color4i.fromRGB(BiomeColorHelper.getWaterColorAtPos(world, colorPos));
                        } else if (block == Blocks.GRASS_PATH || block == Blocks.DIRT || block == Blocks.MYCELIUM) {
                            mapColor = Color4i.fromRGB(BiomeColorHelper.getGrassColorAtPos(world, colorPos));
                        } else {
                            mapColor = Color4i.fromRGB(state.getMapColor(world, colorPos).colorValue);
                        }

                        rawChunk17[index] = mapColor.toRGB();
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
        int VERT_OFFSET = 13;
        int WIDTH = (16 * 4) * 5 + (2 * offset);
        int HEIGHT = (16 * 4) * 5 + VERT_OFFSET + (int) (2.5 * offset);
        ModularPanel panel = ModularPanel.defaultPanel("siege_main")
                .width(WIDTH)
                .height(HEIGHT)
                .topRel(0.40f);


        panel.child(new ButtonWidget<>()
                .background(GuiTextures.BUTTON_CLEAN)
                .overlay(GuiTextures.CLOSE)
                .onMousePressed(mouseButton -> {
                            panel.closeIfOpen(true);
                            return true;
                        }
                )
                .width(12)
                .height(12)
                .pos(WIDTH - offset * 3, (offset / 2) + 1)
        );

        panel.child(IKey.str("Select chunk to siege").asWidget()
                .pos(offset, offset)
        );

        panel.child(IKey.str("Current momentum: " + momentum).asWidget()
                .pos(WIDTH - (offset + 12 + 5 + 100), offset)
                .tooltip(richTooltip -> {
                    richTooltip.addLine("§6Momentum System§r");
                    richTooltip.addLine("§7Momentum affects siege duration.§r");
                    richTooltip.addLine("§7Gained via §aWON§7 sieges, lost on §cLOST§7 sieges.§r");
                    richTooltip.addLine("§7Expires after §b" + WarForgeConfig.SIEGE_MOMENTUM_DURATION + " minutes§7.§r");

                    //FIXME
                    richTooltip.addLine("§eLevel 0"  + "§r: §a" + (float)WarForgeConfig.SIEGE_MOMENTUM_TIME.get((byte)0)/60 + " minutes§r");

                    for (byte level : WarForgeConfig.SIEGE_MOMENTUM_TIME.keySet()) {
                        float time = WarForgeConfig.SIEGE_MOMENTUM_TIME.get(level);
                        long adjusted = Math.round(time/60); // convert ms → minutes
                        richTooltip.addLine("§eLevel " + level + "§r: §a" + adjusted + " minutes§r");
                    }
                })
        );

        int id = 0;

        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {

                int finalId = id;
                SiegeCampAttackInfoRender chunkInfo = new SiegeCampAttackInfoRender(possibleAttacks.get(finalId));
                panel.child(new ButtonWidget<>()
                        .overlay(new MapDrawable("chunk" + id, chunkInfo, adjesencyArray[id]))
                        .onMousePressed(mouseButton -> {
                            if ((chunkInfo.mOffset.getX() == 0 && chunkInfo.mOffset.getZ() == 0) && !chunkInfo.canAttack)
                                return false;
                            //player.sendMessage(new TextComponentString(chunkInfo.mOffset.toString()));  debug vec dis
                            PacketStartSiege siegePacket = new PacketStartSiege();

                            siegePacket.mSiegeCampPos = siegeCampPos;
                            siegePacket.mOffset = chunkInfo.mOffset;

                            WarForgeMod.NETWORK.sendToServer(siegePacket);
                            panel.closeIfOpen(true);
                            return true;
                        })
                        .tooltip(richTooltip ->
                        {
                            if (!chunkInfo.mFactionName.isEmpty()) {
                                richTooltip.addLine(IKey.str("Territory of " + chunkInfo.mFactionName).color(Color4i.fromRGB(chunkInfo.mFactionColour).toARGB()));
                            } else {
                                richTooltip.addLine(IKey.str("Wilderness").style(IKey.GREEN));
                            }
                            if (chunkInfo.mWarforgeVein != null) {

                                richTooltip.addLine(new IngredientDrawable(
                                        chunkInfo.mWarforgeVein.compIds.stream()
                                                .map(StackComparable::toItem)
                                                .filter(Objects::nonNull)
                                                .toArray(ItemStack[]::new)
                                ).asIcon().size(25));
                                richTooltip.addLine(IKey.str("Ore In the chunk: " + I18n.format(chunkInfo.mWarforgeVein.translationKey, I18n.format(chunkInfo.mOreQuality.getTranslationKey()))));
                            } else {
                                richTooltip.addLine(IKey.str("No ores in this chunk"));
                            }
                            if (chunkInfo.canAttack) {
                                richTooltip.addLine(IKey.str("Attack time: " + WarForgeConfig.SIEGE_MOMENTUM_TIME.get(momentum) + " minutes").style( IKey.RED));
                                richTooltip.addLine(IKey.str("Click to attack now!").style(IKey.BOLD, IKey.RED));
                            }
                        })
                        .size(16 * 4)
                        .pos((i * (16 * 4) + offset), (j * (16 * 4) + offset) + VERT_OFFSET));
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

