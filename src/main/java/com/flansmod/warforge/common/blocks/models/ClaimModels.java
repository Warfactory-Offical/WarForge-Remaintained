package com.flansmod.warforge.common.blocks.models;

import com.flansmod.warforge.client.models.ThemableBakedModel;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.common.util.IDynamicModels;
import com.google.common.collect.ImmutableMap;
import lombok.SneakyThrows;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.TRSRTransformation;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;


public class ClaimModels implements IDynamicModels {

    public static final String texturePath = "textures/blocks/statues/";
    public static final Map<ModelType, IBakedModel[]> MODEL_MAP = new EnumMap<>(ModelType.class);
    public static ResourceLocation BASE_STATUE = new ResourceLocation(WarForgeMod.MODID, "block/dummy/statue_base");

    public ClaimModels() {
        INSTANCES.add(this);
    }

    private static ThemableBakedModel[] bakeTheamableModels(Map<THEME, IModel> modelMap) {
        return bakeTheamableModels(modelMap, 8);
    }

    private static ThemableBakedModel[] bakeTheamableModels(Map<THEME, IModel> modelMap, int steps) {
        var array = new ThemableBakedModel[steps];
        for (int i = 0; i < steps; i++) {
            float angle = i * (360f / steps);
            Map<THEME, IBakedModel> bakedModelMap = new HashMap<>();
            for (THEME theme : modelMap.keySet()) {
                var model = modelMap.get(theme);
                TRSRTransformation rotation = TRSRTransformation.blockCenterToCorner(
                        new TRSRTransformation(
                                null,
                                TRSRTransformation.quatFromXYZ(0, (float) Math.toRadians(angle), 0),
                                null,
                                null
                        )
                );
                bakedModelMap.put(theme,
                        model.bake(rotation, DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter()
                        ));

            }
            array[i] = new ThemableBakedModel(bakedModelMap);
        }

        return array;
    }


    @SneakyThrows
    @Override
    public void bakeModel(ModelBakeEvent event) {
        IModel baseStatueModel = ModelLoaderRegistry.getModel(BASE_STATUE);
        String classicTexturePath = "blocks/statues/classic";
        String modernModelPath = "block/statues/modern";

        Map<THEME, IModel> citadelModels = ImmutableMap.of(
                THEME.MEDIVAL, baseStatueModel.retexture(
                        ImmutableMap.of("0", new ResourceLocation(WarForgeMod.MODID, classicTexturePath + "/statue_king").toString())
                ),
                THEME.MODERN, ModelLoaderRegistry.getModelOrMissing(new ResourceLocation(WarForgeMod.MODID, modernModelPath + "/flag_pole"))
        );

        // Use your themable baking for CITADEL
        MODEL_MAP.put(ModelType.CITADEL, bakeTheamableModels(citadelModels));

        Map<THEME, IModel> basicClaimModels = ImmutableMap.of(
                THEME.MEDIVAL, baseStatueModel.retexture(
                        ImmutableMap.of("0", new ResourceLocation(WarForgeMod.MODID, classicTexturePath + "/statue_knight").toString())
                ),
                THEME.MODERN, ModelLoaderRegistry.getModelOrMissing(new ResourceLocation(WarForgeMod.MODID, modernModelPath + "/radio"))
        );
        MODEL_MAP.put(ModelType.BASIC_CLAIM, bakeTheamableModels(basicClaimModels));

        // SIEGE using same placeholder models
        Map<THEME, IModel> siegeModels = ImmutableMap.of(
                THEME.MEDIVAL, baseStatueModel.retexture(
                        ImmutableMap.of("0", new ResourceLocation(WarForgeMod.MODID, classicTexturePath + "/statue_berserker").toString())
                ),
                THEME.MODERN, baseStatueModel.retexture(
                        ImmutableMap.of("0", new ResourceLocation(WarForgeMod.MODID, classicTexturePath + "/statue_berserker").toString())
                )
        );
        MODEL_MAP.put(ModelType.SIEGE, bakeTheamableModels(siegeModels));
    }

    @Override
    public void registerModel() {

    }

    @Override
    public void registerSprite(TextureMap map) {
        var spriteList = DataRetrivalUtil.getResourcesFromPath(texturePath);
        spriteList.forEach(map::registerSprite);
    }

    public enum ModelType {
        CITADEL("citadel"),
        BASIC_CLAIM("basic_claim"),
        SIEGE("siege");

        private final String id;

        ModelType(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    public enum THEME {
        MEDIVAL, MODERN;
    }

}
