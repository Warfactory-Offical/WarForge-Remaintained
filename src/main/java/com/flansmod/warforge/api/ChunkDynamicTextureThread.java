package com.flansmod.warforge.api;

import com.flansmod.warforge.common.WarForgeMod;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ChunkDynamicTextureThread extends Thread {
    public static Queue<RegisterTextureAction> queue = new ConcurrentLinkedQueue<>();
    final int[] rawChunk;
    final int[] heightMapCopy;
    final int maxHeight;
    final int minHeight;
    int scale;
    String name;

    public ChunkDynamicTextureThread(int scale, String name, int[] rawChunk1, int[] heightMapCopy1, int maxHeight, int minHeight) {
        this.scale = scale;
        this.name = name;
        this.rawChunk = rawChunk1;
        this.heightMapCopy = heightMapCopy1;
        this.maxHeight = maxHeight;
        this.minHeight = minHeight;
    }

    public static int[] scaleRGBAArray(int[] originalPixels, int originalWidth, int originalHeight, int scale) {
        int newWidth = originalWidth * scale;
        int newHeight = originalHeight * scale;
        int[] scaledPixels = new int[newWidth * newHeight];

        for (int y = 0; y < originalHeight; y++) {
            for (int x = 0; x < originalWidth; x++) {
                int color = originalPixels[x + y * originalWidth];

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

    public static void applyShadingWithHeight(int[] rawChunk, int[] heightMap, int width, int height) {
        int[] shaded = new int[rawChunk.length];

        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int idx = x + z * width;
                int baseColor = rawChunk[idx];
                int baseHeight = heightMap[idx];

                int idxEast = (x < width - 1) ? idx + 1 : idx;
                int idxSouth = (z < height - 1) ? idx + width : idx;

                int eastHeight = heightMap[idxEast];
                int southHeight = heightMap[idxSouth];

                int heightDiffEast = baseHeight - eastHeight;
                int heightDiffSouth = baseHeight - southHeight;

                float shadeFactor = 1.0f - ((heightDiffEast + heightDiffSouth) / 2f) * 0.2f;
                shadeFactor = Math.max(0.7f, Math.min(1.0f, shadeFactor));

                shaded[idx] = applyBrightness(baseColor, shadeFactor);
            }
        }
        System.arraycopy(shaded, 0, rawChunk, 0, rawChunk.length);
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

    @Override
    public void run() {
        int padded = 17;
        int paddedScaled = padded * scale;

        applyHeightMap(rawChunk, heightMapCopy);

        int[] scaled = scaleRGBAArray(rawChunk, padded, padded, scale);

        applyShadingWithHeight(scaled, scaleRGBAArray(heightMapCopy, 17, 17, 4), paddedScaled, paddedScaled);

        // Crop 16×16 center
        int[] finalBuffer = new int[16 * scale * 16 * scale];
        for (int z = 0; z < 16 * scale; z++) {
            int srcOffset = (z + scale) * paddedScaled + scale;
            int dstOffset = z * 16 * scale;
            System.arraycopy(scaled, srcOffset, finalBuffer, dstOffset, 16 * scale);
        }

        // Final image
        BufferedImage image = new BufferedImage(16 * scale, 16 * scale, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 16 * scale, 16 * scale, finalBuffer, 0, 16 * scale);
        queue.add(new RegisterTextureAction(image, name));
    }


    public void applyHeightMap(int[] colorBuffer, int[] heightMap) {

        for (int i = 0; i < colorBuffer.length; i++) {
            int rgb = colorBuffer[i];

            // Normalize height: 0.0 to 1.0
            float normalized = (float) Math.log(heightMap[i] - minHeight + 1) / (float) Math.log(maxHeight - minHeight + 1);

            // Extract original RGB
            int alpha = (rgb >>> 24) & 0xFF;
            int red = (rgb >>> 16) & 0xFF;
            int green = (rgb >>> 8) & 0xFF;
            int blue = (rgb) & 0xFF;

            // Apply brightness multiplier
            float brightness = 0.6f + normalized * 0.4f; // keep brightness in [0.6, 1.0]

            red = (int) (red * brightness);
            green = (int) (green * brightness);
            blue = (int) (blue * brightness);

            // Clamp to [0,255]
            red = Math.min(255, red);
            green = Math.min(255, green);
            blue = Math.min(255, blue);

            colorBuffer[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }


    }

    @RequiredArgsConstructor
    public static class RegisterTextureAction {
        final BufferedImage mapTexture;
        final String name;

        public void register() {
            Minecraft.getMinecraft().getTextureManager().loadTexture(
                    new ResourceLocation(WarForgeMod.MODID, name),
                    new DynamicTexture(mapTexture)
            );
        }


    }
}