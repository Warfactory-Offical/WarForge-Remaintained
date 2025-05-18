package com.flansmod.warforge.client.util;


import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.blocks.BlockDummy;
import com.flansmod.warforge.common.blocks.TileEntityCitadel;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class RenderTileEntityCitadel extends TileEntitySpecialRenderer<TileEntityCitadel> {


    private final IBlockState blockState = Content.statue.getDefaultState().withProperty(BlockDummy.MODEL, BlockDummy.modelEnum.KING);
    private final IBakedModel model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(blockState);


    @Override
    public void render(TileEntityCitadel te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {

        GlStateManager.pushMatrix();
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        double centerX = x+0.50d;
        double centerZ = z-0.50d;
        double angleRAD = Math.toRadians(te.rotation);

        double vectorX = centerX - x;
        double vectorZ = centerZ - z;
        double cosA = Math.cos(angleRAD);
        double sinA = Math.sin(angleRAD);
       double vecXRotation = vectorX * cosA - vectorZ * sinA;
       double vecZRotation = vectorX * sinA + vectorZ * cosA;

        GlStateManager.shadeModel(GL11.GL_SMOOTH);
        GlStateManager.translate(x+vecXRotation,y+1,z+vecZRotation);

        //GlStateManager.translate(x+ (Math.sin(te.rotation+partialTicks)), y+1, z * (Math.cos(te.rotation+partialTicks)));
//        GlStateManager.rotate(te.rotation+partialTicks, 0, 1, 0);

        GlStateManager.rotate(te.rotation+partialTicks, 0, 1, 0);

        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightness(model, blockState, 1, false);

        GlStateManager.popMatrix();
    }

    @Override
    public boolean isGlobalRenderer(TileEntityCitadel cita) {
        return true;
    }
}
