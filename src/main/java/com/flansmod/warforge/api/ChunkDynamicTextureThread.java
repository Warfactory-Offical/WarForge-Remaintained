package com.flansmod.warforge.api;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.lang.reflect.Field;

public class ChunkDynamicTextureThread extends Thread {
    int scale;
    DynamicTexture mapTexture;
    String name;
    final int[] rawChunk;
    final int[] heightMapCopy;

    public ChunkDynamicTextureThread(int scale, String name, int[] rawChunk1, int[] heightMapCopy1) {
        this.scale = scale;
        this.name = name;
        this.rawChunk = rawChunk1;
        this.heightMapCopy = heightMapCopy1;
    }

    @Override
    public void run() {
        int[] scaledBuffer = scaleRGBAArray(rawChunk, 16, 16, scale);
        int[] scaledHeightMap = scaleRGBAArray(heightMapCopy, 16, 16, scale);
        applyShading(scaledBuffer, 16 * scale, 16 * scale);

        if (mapTexture == null) {
            mapTexture = new DynamicTexture(16 * scale, 16 * scale);
            Minecraft.getMinecraft().getTextureManager().loadTexture(new ResourceLocation(WarForgeMod.MODID, name), mapTexture);
        }

        try {
            Field textureDataField = DynamicTexture.class.getDeclaredField("dynamicTextureData");
            textureDataField.setAccessible(true);
            textureDataField.set(mapTexture, scaledBuffer);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        mapTexture.updateDynamicTexture();

    }


    public static int[] scaleRGBAArray(int[] originalPixels, int originalWidth, int originalHeight, int scale) {
        int newWidth = originalWidth * scale;
        int newHeight = originalHeight * scale;
        int[] scaledPixels = new int[newWidth * newHeight];

        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                int color = originalPixels[x + y * originalWidth];

                // Fill scale x scale block in scaledPixels
                int startX = x * scale;
                int startY = y * scale;

                for (int dy = 0; dy < scale; dy++) {
                    for (int dx = 0; dx < scale; dx++) {
                        int scaledIndex = (startX + dx) + (startY + dy) * newWidth;
                        scaledPixels[scaledIndex] = color;
                    }
                }
            }
        }

        return scaledPixels;
    }

    public static void applyShading(int[] rawChunk, int width, int height) {
        int[] shaded = new int[rawChunk.length];

        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int idx = x + z * width;
                int baseColor = rawChunk[idx];

                // Avoid edge overflow by clamping neighbors
                int idxEast = (x < width - 1) ? idx + 1 : idx;
                int idxSouth = (z < height - 1) ? idx + width : idx;

                // Extract brightness from color (simple approximation)
                int baseBrightness = brightness(rawChunk[idx]);
                int eastBrightness = brightness(rawChunk[idxEast]);
                int southBrightness = brightness(rawChunk[idxSouth]);

                // Calculate brightness difference (a simple lighting gradient)
                int diffEast = baseBrightness - eastBrightness;
                int diffSouth = baseBrightness - southBrightness;

                // Combine diffs to get shading factor (clamped)
                float shadeFactor = 1.0f - (diffEast + diffSouth) * 0.05f;
                shadeFactor = Math.max(0.6f, Math.min(1.0f, shadeFactor)); // Clamp between 0.6 and 1.0

                // Apply shading by scaling RGB channels
                shaded[idx] = applyBrightness(baseColor, shadeFactor);
            }
        }

        // Copy back shaded colors to original buffer
        System.arraycopy(shaded, 0, rawChunk, 0, rawChunk.length);
    }

    // Helper: approximate brightness as average of RGB channels
    private static int brightness(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r + g + b) / 3;
    }

    // Helper: multiply RGB by brightness factor, keep alpha unchanged
    private static int applyBrightness(int color, float factor) {
        int alpha = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);

        // Clamp RGB
        r = Math.min(255, Math.max(0, r));
        g = Math.min(255, Math.max(0, g));
        b = Math.min(255, Math.max(0, b));

        return (alpha << 24) | (r << 16) | (g << 8) | b;
    }


}