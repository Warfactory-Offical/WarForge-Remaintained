package com.flansmod.warforge.client.models;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraftforge.common.model.TRSRTransformation;

import javax.vecmath.Vector3f;

public class DefaultTransform {
    public static final ImmutableMap<ItemCameraTransforms.TransformType, TRSRTransformation> BLOCK_TRANSFORMS =
            ImmutableMap.<ItemCameraTransforms.TransformType, TRSRTransformation>builder()
                    .put(ItemCameraTransforms.TransformType.GUI,
                            blockCenterToCorner(30, 225, 0, 0.625f, 0f, 0f, 0f))
                    .put(ItemCameraTransforms.TransformType.GROUND,
                            blockCenterToCorner(0, 0, 0, 0.25f, 0f, 3f, 0f))
                    .put(ItemCameraTransforms.TransformType.FIXED,
                            blockCenterToCorner(0, 0, 0, 0.5f, 0f, 0f, 0f))
                    .put(ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND,
                            blockCenterToCorner(75, 45, 0, 0.375f, 0f, 2.5f, 0f))
                    .put(ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND,
                            blockCenterToCorner(75, 255, 0, 0.375f, 0f, 2.5f, 0f))
                    .put(ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND,
                            blockCenterToCorner(0, 45, 0, 0.4f, 0f, 0f, 0f))
                    .put(ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND,
                            blockCenterToCorner(0, 225, 0, 0.4f, 0f, 0f, 0f))
                    .build();

    private static TRSRTransformation blockCenterToCorner(
            float rotX, float rotY, float rotZ,
            float scale, float transX, float transY, float transZ) {

        return TRSRTransformation.blockCenterToCorner(new TRSRTransformation(
                new Vector3f(transX / 16f, transY / 16f, transZ / 16f),
                TRSRTransformation.quatFromXYZDegrees(new Vector3f(rotX, rotY, rotZ)),
                new Vector3f(scale, scale, scale),
                null
        ));
    }
}
