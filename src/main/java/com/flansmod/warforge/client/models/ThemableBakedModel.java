package com.flansmod.warforge.client.models;

import com.flansmod.warforge.common.WarForgeConfig;
import com.flansmod.warforge.common.blocks.models.ClaimModels;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;

import java.util.List;
import java.util.Map;

public class ThemableBakedModel implements IBakedModel {

    Map<ClaimModels.THEME, IBakedModel> models;

    public ThemableBakedModel(Map<ClaimModels.THEME, IBakedModel> models) {
        this.models = models;
    }

    private IBakedModel getActiveModel() {
        return WarForgeConfig.MODERN_WARFARE_MODELS ? models.get(ClaimModels.THEME.MODERN) : models.get(ClaimModels.THEME.MEDIVAL);
    }

    @Override
    public List<BakedQuad> getQuads(IBlockState state, EnumFacing side, long rand) {
        return getActiveModel().getQuads(state, side, rand);
    }

    @Override
    public boolean isAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return getActiveModel().getParticleTexture();
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    public org.apache.commons.lang3.tuple.Pair<? extends IBakedModel, javax.vecmath.Matrix4f> handlePerspective(ItemCameraTransforms.TransformType cameraTransformType) {
        return getActiveModel().handlePerspective(cameraTransformType);

    }

   public ItemCameraTransforms getItemCameraTransforms() { return getActiveModel().getItemCameraTransforms(); }

}
