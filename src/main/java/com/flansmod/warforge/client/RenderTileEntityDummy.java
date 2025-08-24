package com.flansmod.warforge.client;

import com.flansmod.warforge.common.blocks.TileEntityDummy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;

import static net.minecraft.client.renderer.tileentity.TileEntityBeaconRenderer.renderBeamSegment;

public class RenderTileEntityDummy extends TileEntitySpecialRenderer<com.flansmod.warforge.common.blocks.TileEntityDummy> {

    public void render(com.flansmod.warforge.common.blocks.TileEntityDummy te, double x, double y, double z, float partialTicks, int destroyStage, float f) {

        if (te.getLaserRender()) {
            GlStateManager.pushMatrix();

            GlStateManager.disableLighting();
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableFog();

            Minecraft.getMinecraft().renderEngine.bindTexture(TileEntityBeaconRenderer.TEXTURE_BEACON_BEAM);

            double time = te.getWorld().getTotalWorldTime();

            float[] color = te.getLaserRGB();

            int height = 256 - te.getPos().getY();

            renderBeamSegment(x, y + 1, z, partialTicks, 1, time, 0, height, color, 0.2, 0.25);

            GlStateManager.enableFog();
            GlStateManager.enableLighting();

            GlStateManager.popMatrix();
        }
    }


    public boolean isGlobalRenderer(TileEntityDummy te) {
        return true;
    }

}
