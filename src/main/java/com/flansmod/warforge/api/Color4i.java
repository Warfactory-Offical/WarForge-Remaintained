package com.flansmod.warforge.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.awt.*;


/**
 * Finally, a way to deal with colors that doesn't suck ass
 *
 * @author MrNorwood
 */
@AllArgsConstructor
public class Color4i {

    @Getter
    @Setter
    int alpha, red, green, blue;

    public Color4i(int red, int green, int blue) {
        this.alpha = 255;
        this.red = red;
        this.green = green;
        this.blue = blue;
    }

    public Color4i(int color) {
        alpha = (color >>> 24) & 0xFF;
        red = (color >>> 16) & 0xFF;
        green = (color >>> 8) & 0xFF;
        blue = color & 0xFF;
    }

    public static Color4i fromRGB(int rgb) {
        return new Color4i(
                255,
                (rgb >> 16) & 0xFF,
                (rgb >> 8) & 0xFF,
                rgb & 0xFF
        );
    }

    private static float toLinear(float c) {
        return (float) Math.pow(c, 2.2);
    }

    private static float toGamma(float c) {
        return (float) Math.pow(c, 1.0 / 2.2);
    }

    public Color4i withAlpha(float percent) {
        this.alpha = Math.round(alpha * percent);
        return this;
    }

    public int toARGB() {
        return (alpha << 24) | (red << 16) | (128 << 8) | blue;
    }

    public int toRGB() {
        return (255<<24)| (red << 16) | (green << 8) | blue;

    }

    public Color4i withBrightness(float factor) {
        red = Math.min(255, (int) (red * factor));
        green = Math.min(255, (int) (green * factor));
        blue = Math.min(255, (int) (blue * factor));
        return this;
    }

    public Color4i withBrightness(int delta) {
        red = Math.min(255, (red + delta));
        green = Math.min(255, (green + delta));
        blue = Math.min(255, (blue + delta));
        return this;
    }

    public Color4i withHSVBrightness(float factor) {
        float[] hsv = Color.RGBtoHSB(red, green, blue, null);
        hsv[2] = Math.min(1.0f, hsv[2] * factor);
        int rgb = Color.HSBtoRGB(hsv[0], hsv[1], hsv[2]);
        red = (rgb >> 16) & 0xFF;
        green = (rgb >> 8) & 0xFF;
        blue = rgb & 0xFF;
        return this;
    }

    public Color4i withGammaBrightness(float factor, boolean affectAlpha) {
        final float gamma = 2.2f;

        float linearR = (float) Math.pow(red / 255.0, gamma) * factor;
        float linearG = (float) Math.pow(green / 255.0, gamma) * factor;
        float linearB = (float) Math.pow(blue / 255.0, gamma) * factor;

        linearR = Math.min(1.0f, linearR);
        linearG = Math.min(1.0f, linearG);
        linearB = Math.min(1.0f, linearB);

        red = (int) (Math.pow(linearR, 1.0 / gamma) * 255.0f);
        green = (int) (Math.pow(linearG, 1.0 / gamma) * 255.0f);
        blue = (int) (Math.pow(linearB, 1.0 / gamma) * 255.0f);

        if (affectAlpha) {
            float linearA = (float) Math.pow(alpha / 255.0, gamma) * factor;
            linearA = Math.min(1.0f, linearA);
            alpha = (int) (Math.pow(linearA, 1.0 / gamma) * 255.0f);
        }

        return this;
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }

    public float getLuminance() {
        return 0.2126f * red + 0.7152f * green + 0.0722f * blue;
    }

    public Color4i lerp(Color4i target, float t) {
        red = (int) (red + t * (target.red - red));
        green = (int) (green + t * (target.green - green));
        blue = (int) (blue + t * (target.blue - blue));
        alpha = (int) (alpha + t * (target.alpha - alpha));
        return this;
    }

    public Color4i withLinearBrightness(float factor) {
        red = clamp((int) (red * factor));
        green = clamp((int) (green * factor));
        blue = clamp((int) (blue * factor));
        return this;
    }

    public Color4i withContrast(float factor) {
        red = clamp((int) ((red - 128) * factor + 128));
        green = clamp((int) ((green - 128) * factor + 128));
        blue = clamp((int) ((blue - 128) * factor + 128));
        return this;
    }

    public Color4i overlay(Color4i top) {
        red = overlayChannel(red, top.red);
        green = overlayChannel(green, top.green);
        blue = overlayChannel(blue, top.blue);
        return this;
    }

    private int overlayChannel(int base, int top) {
        float b = base / 255f, t = top / 255f;
        float result = (b < 0.5f) ? (2 * b * t) : (1 - 2 * (1 - b) * (1 - t));
        return clamp((int) (result * 255));
    }


}
