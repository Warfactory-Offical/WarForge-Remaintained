package com.flansmod.warforge.client.models;

import com.flansmod.warforge.common.blocks.models.ClaimModels;
import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.model.ModelRotation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.PerspectiveMapWrapper;

public class BakingUtil {
    public static void registerFacingModels(
            IModel medieval, IModel modern,
            IRegistry<ModelResourceLocation, IBakedModel> registry,
            ResourceLocation baseLoc) {

        ModelResourceLocation invLoc;
        for (EnumFacing facing : EnumFacing.HORIZONTALS) {
            ModelRotation rotation = switch (facing) {
                case SOUTH -> ModelRotation.X0_Y180;
                case WEST -> ModelRotation.X0_Y270;
                case EAST -> ModelRotation.X0_Y90;
                default -> ModelRotation.X0_Y0;
            };

            IBakedModel themed = bakeThemed(medieval, modern, rotation);

            ModelResourceLocation loc = new ModelResourceLocation(
                    baseLoc, "facing=" + facing.getName().toLowerCase()
            );
            if (facing.equals(EnumFacing.NORTH)) {
                invLoc = new ModelResourceLocation(baseLoc, "inventory");
                registry.putObject(invLoc, themed);
            }
            registry.putObject(loc, themed);
        }

    }

    public static IBakedModel bakeThemed(IModel medival, IModel modern, ModelRotation rotation) {
        return new PerspectiveMapWrapper(new ThemableBakedModel(ImmutableMap.of(
                ClaimModels.THEME.MEDIVAL, bake(medival, rotation),
                ClaimModels.THEME.MODERN, bake(modern, rotation)
        )), DefaultTransform.BLOCK_TRANSFORMS);

    }

    public static IBakedModel bake(IModel model, ModelRotation rotation) {
        return new PerspectiveMapWrapper(model.bake(rotation, DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()), DefaultTransform.BLOCK_TRANSFORMS);
    }

}
