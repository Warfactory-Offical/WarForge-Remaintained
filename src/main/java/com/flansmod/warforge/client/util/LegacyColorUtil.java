package com.flansmod.warforge.client.util;

public class LegacyColorUtil {

    public static final int[][] LEGACY_RGB = {
            {0, 0, 0},         // §0
            {0, 0, 170},       // §1
            {0, 170, 0},       // §2
            {0, 170, 170},     // §3
            {170, 0, 0},       // §4
            {170, 0, 170},     // §5
            {255, 170, 0},     // §6
            {170, 170, 170},   // §7
            {85, 85, 85},      // §8
            {85, 85, 255},     // §9
            {85, 255, 85},     // §a
            {85, 255, 255},    // §b
            {255, 85, 85},     // §c
            {255, 85, 255},    // §d
            {255, 255, 85},    // §e
            {255, 255, 255}    // §f
    };

    public static final char[] LEGACY_CODES = {
            '0', '1', '2', '3', '4', '5', '6', '7',
            '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'
    };

    public static String getClosestLegacyColor(int rgb) {
        int r1 = (rgb >> 16) & 0xFF;
        int g1 = (rgb >> 8) & 0xFF;
        int b1 = rgb & 0xFF;

        int closestIndex = 15; // default to white
        int minDist = Integer.MAX_VALUE;

        for (int i = 0; i < LEGACY_RGB.length; i++) {
            int[] c = LEGACY_RGB[i];
            int dr = r1 - c[0];
            int dg = g1 - c[1];
            int db = b1 - c[2];
            int dist = dr * dr + dg * dg + db * db;

            if (dist < minDist) {
                minDist = dist;
                closestIndex = i;
            }
        }

        return "§" + LEGACY_CODES[closestIndex];
    }
}
