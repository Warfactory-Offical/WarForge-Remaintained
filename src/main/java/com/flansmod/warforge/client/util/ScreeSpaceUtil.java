package com.flansmod.warforge.client.util;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

@SideOnly(Side.CLIENT)
public class ScreeSpaceUtil {


    public static int RESOLUTIONY;
    public static int RESOLUTIONX;
    // Positive = offset downward/right
    // Negative = offset upward/left
    public static int topLeftOffset, topRightOffset, topOffset;
    public static int bottomLeftOffset, bottomRightOffset, bottomOffset;
    static Minecraft minecraft = Minecraft.getMinecraft();
    public static int TEXTHEIGHT = minecraft.fontRenderer.FONT_HEIGHT;

    public static void resetOffsets(RenderGameOverlayEvent event) {
        RESOLUTIONX = event.getResolution().getScaledWidth();
        RESOLUTIONY = event.getResolution().getScaledHeight();
        topOffset = topLeftOffset = topRightOffset = 0;
        bottomOffset = RESOLUTIONY - 56;
        bottomLeftOffset = bottomRightOffset = RESOLUTIONY;
    }

    public static int centerX(int screenWidth, int elementWidth) {
        return (screenWidth - elementWidth) / 2;
    }

    public static int centerY(int screenHeight, int elementHeight) {
        return (screenHeight - elementHeight) / 2;
    }

    public static boolean isTop(ScreenPos pos) {
        return switch (pos) {
            case TOP, TOP_LEFT, TOP_RIGHT -> true;
            default -> false;
        };
    }

    public static int getXOffset(ScreenPos pos, int offset) {
        return switch (pos) {
            case TOP_RIGHT, BOTTOM_RIGHT -> -offset;
            case TOP, BOTTOM -> 0;
            default -> offset;
        };
    }
    public static int getYOffset(ScreenPos pos, int offset) {
        return switch (pos) {
            case TOP, TOP_RIGHT, TOP_LEFT -> offset;
            default -> -offset;
        };
    }


    public static int getX(ScreenPos pos, int elementWidth) {
        int screenWidth = RESOLUTIONX;
        return switch (pos) {
            case TOP_LEFT, BOTTOM_LEFT -> 0;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - elementWidth;
            default -> centerX(screenWidth, elementWidth);
        };
    }

    public static int getY(ScreenPos pos, int elementHeight) {
        switch (pos) {
            case TOP:
            case TOP_LEFT:
            case TOP_RIGHT:
                int offset = topOffset;
                topOffset += elementHeight;
                return offset;
            case BOTTOM:
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                bottomOffset -= elementHeight;
                return bottomOffset;
            default:
                return centerY(RESOLUTIONY, elementHeight);
        }
    }

    public static void incrementY(ScreenPos pos, int amount) {
        switch (pos) {
            case TOP -> topOffset += amount;
            case TOP_LEFT -> topLeftOffset += amount;
            case TOP_RIGHT -> topRightOffset += amount;
            case BOTTOM -> bottomOffset -= amount;
            case BOTTOM_LEFT -> bottomLeftOffset -= amount;
            case BOTTOM_RIGHT -> bottomRightOffset -= amount;
        }
    }

    public static boolean shouldCenterX(ScreenPos pos) {
        return pos == ScreenPos.TOP || pos == ScreenPos.BOTTOM;
    }


    public enum ScreenPos {
        TOP_LEFT(
                () -> 0,
                () -> topLeftOffset,
                val -> topLeftOffset = val
        ),
        BOTTOM_LEFT(
                () -> 0,
                () -> bottomLeftOffset,
                val -> bottomLeftOffset = val
        ),
        TOP(
                () -> centerX(RESOLUTIONX, 0),
                () -> topOffset,
                val -> topOffset = val
        ),
        BOTTOM(
                () -> centerX(RESOLUTIONX, 0),
                () -> bottomOffset,
                val -> bottomOffset = val
        ),
        TOP_RIGHT(
                () -> RESOLUTIONX,
                () -> topRightOffset,
                val -> topRightOffset = val
        ),
        BOTTOM_RIGHT(
                () -> RESOLUTIONX,
                () -> bottomRightOffset,
                val -> bottomRightOffset = val
        );

        private final IntSupplier xSupplier;
        private final IntSupplier ySupplier;
        private final IntConsumer ySetter;

        ScreenPos(IntSupplier xSupplier, IntSupplier ySupplier, IntConsumer ySetter) {
            this.xSupplier = xSupplier;
            this.ySupplier = ySupplier;
            this.ySetter = ySetter;
        }

        public int getX() {
            return xSupplier.getAsInt();
        }

        public int getY() {
            return ySupplier.getAsInt();
        }

        public void setY(int newY) {
            ySetter.accept(newY);
        }

        public void incrementY(int delta) {
            setY(getY() + delta);
        }
    }
}

