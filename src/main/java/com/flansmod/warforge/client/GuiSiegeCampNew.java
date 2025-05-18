package com.flansmod.warforge.client;

import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.flansmod.warforge.api.MapDrawable;
import com.flansmod.warforge.common.DimBlockPos;
import com.flansmod.warforge.common.network.SiegeCampAttackInfo;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapData;

import java.util.List;

public class GuiSiegeCampNew {

    public static ModularScreen makeGUI
            (DimBlockPos siegeCampPos, List<SiegeCampAttackInfo> possibleAttacks) {

        EntityPlayer player = Minecraft.getMinecraft().player;
        World world = player.world;
        int playerChunkX = player.chunkCoordX;
        int playerChunkZ = player.chunkCoordY;
        int mapCenterX = (playerChunkX << 4) + 8;
        int mapCenterZ = (playerChunkZ << 4) + 8;
        MapData data = new MapData("local_area");
        data.scale = 3;
        data.xCenter = mapCenterX;
        data.zCenter = mapCenterZ;
        data.dimension = world.provider.getDimension();
        data.colors = new byte[128 * 128];
        for (int px = 0; px < 128; px++) {
            for (int pz = 0; pz < 128; pz++) {
                int worldX = mapCenterX + (px - 64) * 8;
                int worldZ = mapCenterZ + (pz - 64) * 8;

                Chunk chunk = world.getChunk(new BlockPos(worldX, 0, worldZ));
                int y = chunk.getHeightValue(worldX & 15, worldZ & 15);
                BlockPos pos = new BlockPos(worldX, y - 1, worldZ);
                IBlockState state = world.getBlockState(pos);
                MapColor color = state.getMapColor(world, pos);

                // Use a grayscale shade (vanilla uses color index and brightness)
                byte mapColorByte = (byte) color.colorIndex;
                data.colors[px + pz * 128] = mapColorByte;
            }
        }


        ModularPanel panel = ModularPanel.defaultPanel("citadel_upgrade_panel")
                .width(500)
                .height(500)
                .child(new MapDrawable(data, Minecraft.getMinecraft().entityRenderer.getMapItemRenderer()).asWidget().size(128, 128));
        return new ModularScreen(panel);
    }

}

