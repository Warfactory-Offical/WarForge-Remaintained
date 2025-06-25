package com.flansmod.warforge.client.util;

import lombok.AllArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

@SideOnly(Side.CLIENT)
public class ScreeSpaceUtil {


        static Minecraft minecraft = Minecraft.getMinecraft();
        public static int RESOLUTIONY ;
        public static int RESOLUTIONX ;
        public static int TEXTHEIGHT = minecraft.fontRenderer.FONT_HEIGHT;

        // Positive = offset downward/right
        // Negative = offset upward/left
        public static int topLeftOffset, topRightOffset, topOffset;
        public static int bottomLeftOffset, bottomRightOffset, bottomOffset;

        public static void resetOffsets(RenderGameOverlayEvent event) {
            RESOLUTIONX = event.getResolution().getScaledWidth();
            RESOLUTIONY = event.getResolution().getScaledHeight();
            topOffset = topLeftOffset = topRightOffset = 0;
            bottomOffset = RESOLUTIONY - 65;
            bottomLeftOffset = bottomRightOffset = RESOLUTIONY;
        }

        public static int centerX(int screenWidth, int elementWidth) {
            return (screenWidth - elementWidth) / 2;
        }

        public static int centerY(int screenHeight, int elementHeight) {
            return (screenHeight - elementHeight) / 2;
        }
    public static int getX(ScreenPos pos, int elementWidth) {
        int screenWidth = RESOLUTIONX;
        switch (pos) {
            case TOP_LEFT:
            case BOTTOM_LEFT:
                return 0;
            case TOP_RIGHT:
            case BOTTOM_RIGHT:
                return screenWidth - elementWidth;
            case TOP:
            case BOTTOM:
            default:
                return centerX(screenWidth, elementWidth);
        }
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
            case TOP:
            case TOP_LEFT:
            case TOP_RIGHT:
                topOffset += amount;
                break;
            case BOTTOM:
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                bottomOffset -= amount;
                break;
            default:
                // No-op or throw
                break;
        }
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

