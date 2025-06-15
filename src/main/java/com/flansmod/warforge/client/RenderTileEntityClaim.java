package com.flansmod.warforge.client;

import com.flansmod.warforge.common.Content;
import com.flansmod.warforge.common.blocks.BlockDummy;
import com.flansmod.warforge.common.blocks.TileEntityClaim;
import com.flansmod.warforge.server.Faction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import org.lwjgl.opengl.GL11;

public class RenderTileEntityClaim extends TileEntitySpecialRenderer<TileEntityClaim> {
    public RenderTileEntityClaim(BlockDummy.modelEnum anEnum) {
        this.anEnum = anEnum;
    }

    private BlockDummy.modelEnum anEnum;
    private  IBlockState blockState;
    private  IBakedModel model;
    public void init()
    {
        blockState = Content.statue.getDefaultState().withProperty(BlockDummy.MODEL, anEnum);
        model = Minecraft.getMinecraft().getBlockRendererDispatcher().getModelForState(blockState);
    }
    @Override
    public void render(TileEntityClaim te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        if(blockState == null) init(); //Jank nation
        if(te.getFaction().equals(Faction.nullUuid)) return;
        GlStateManager.pushMatrix();
        this.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.shadeModel(GL11.GL_SMOOTH);

        GlStateManager.translate(x, y + 1, z + 1);
        GlStateManager.translate(+0.5, 0, -0.5);
        GlStateManager.rotate(te.rotation, 0, 1, 0);
        GlStateManager.translate(-0.5, 0, +0.5);
        Minecraft.getMinecraft().getBlockRendererDispatcher().getBlockModelRenderer().renderModelBrightness(model, blockState, 1, false);

        GlStateManager.popMatrix();
    }

    @Override
    public boolean isGlobalRenderer(TileEntityClaim cita) {
        return true;
    }
}
