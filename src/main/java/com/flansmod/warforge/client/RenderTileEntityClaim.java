package com.flansmod.warforge.client;

import com.flansmod.warforge.common.blocks.TileEntityClaim;
import com.flansmod.warforge.common.blocks.models.ClaimModels;
import com.flansmod.warforge.server.Faction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.animation.FastTESR;

public class RenderTileEntityClaim extends FastTESR<TileEntityClaim> {

    public final ClaimModels.ModelType model ;
    private IBakedModel[] bakedModels;

    public RenderTileEntityClaim(ClaimModels.ModelType model) {
        this.model = model;
        bakedModels = ClaimModels.MODEL_MAP.get(model);
    }


    @Override
    public void renderTileEntityFast(TileEntityClaim te, double x, double y, double z,
                                     float partialTicks, int destroyStage, float alpha,
                                     BufferBuilder buffer) {
        if (te.getFaction().equals(Faction.nullUuid)) return;

        byte index = te.rotation;
        IBakedModel bakedModel = bakedModels[index];
        BlockPos pos = te.getPos();

        buffer.setTranslation(x - pos.getX(), y - pos.getY(), z - pos.getZ());


        IBlockState dummyState = Blocks.STONE.getDefaultState();
        Minecraft.getMinecraft().getBlockRendererDispatcher()
                .getBlockModelRenderer().
                renderModel(te.getWorld(), bakedModel, dummyState, pos.offset(EnumFacing.UP), buffer, false, 0);

        buffer.setTranslation(0, 0, 0);
    }

    @Override
    public boolean isGlobalRenderer(TileEntityClaim te) {
        return true;
    }
}

