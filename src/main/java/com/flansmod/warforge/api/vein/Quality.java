package com.flansmod.warforge.api.vein;

import com.flansmod.warforge.common.WarForgeConfig;

// should be limited to no more than 7 distinct qualities, as anything higher will not fit into the compact storage
public enum Quality {
    // NO MORE THAN 7 DISTINCT LEVELS
    POOR,
    FAIR,
    RICH;

    public static Quality getQuality(int qualityIndex) {
        return Quality.values()[qualityIndex];
    }

    public String getTranslationKey() {
        return "warforge.info.vein." + this.toString().toLowerCase();
    }

    // returns either the integer representation, or a four decimal place floating point representation
    public String getMultString(Vein currVein) {
        // get the quality scaling information and display it alongside the actual quality localized name
        float qualMult = getLocalMultiplier(currVein);
        int qualMultInt = (int) qualMult;
        return (qualMultInt == qualMult ? String.format("%d", qualMultInt) : String.format("%.4f", qualMult)) + "x";
    }

    public float getGlobalMultiplier() {
        return switch (this) {
            case POOR:
                yield WarForgeConfig.POOR_QUAL_MULT;
            case FAIR:
                yield WarForgeConfig.FAIR_QUAL_MULT;
            case RICH:
                yield WarForgeConfig.RICH_QUAL_MULT;
        };
    }

    public float getLocalMultiplier(Vein currVein) {
        if (currVein.qualMults == null) { return getGlobalMultiplier(); }
        if (currVein.qualMults[ordinal()] == -1f) { return getGlobalMultiplier(); }
        return currVein.qualMults[ordinal()];
    }
}
