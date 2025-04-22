package com.flansmod.warforge.client;

import com.flansmod.warforge.common.blocks.TileEntityDummy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntityBeacon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;

import java.util.List;

import static net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer.renderBeamSegment;

public class RenderTileEntityDummy extends TileEntitySpecialRenderer<com.flansmod.warforge.common.blocks.TileEntityDummy> {
    public static final ResourceLocation TEXTURE_BEACON_BEAM = new ResourceLocation("textures/entity/beacon_beam.png");

    public void render(com.flansmod.warforge.common.blocks.TileEntityDummy te, double x, double y, double z, float partialTicks, int destroyStage, float f) {
        if(te.getLaserRender()) {
            GlStateManager.pushMatrix();
            GlStateManager.disableFog();
            Minecraft.getMinecraft().renderEngine.bindTexture(TileEntityBeaconRenderer.TEXTURE_BEACON_BEAM);

            long time = te.getWorld().getTotalWorldTime();
            float[] color = te.getLaserRGB();

            // Height to render the beam to
            int height = 256 - te.getPos().getY();

            // Use copied version of renderBeamSegment
                renderBeamSegment(x, y+1, z, partialTicks, 1, te.getWorld().getTotalWorldTime(), 0, height, color, 0.2, 0.25);

            GlStateManager.enableFog();
            GlStateManager.popMatrix();
        }
    }


    public boolean isGlobalRenderer(TileEntityDummy te){return true;}

}
