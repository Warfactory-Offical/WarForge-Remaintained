package com.flansmod.warforge.api.vein;

// should be limited to no more than 7 distinct qualities, as anything higher will not fit into the compact storage
public enum Quality {
    // NO MORE THAN 7 DISTINCT LEVELS
    RICH,
    FAIR,
    POOR;

    public static Quality getQuality(int qualityIndex) {
        return Quality.values()[qualityIndex];
    }

    public String getTranslationKey() {
        return "warforge.info.vein." + this.toString().toLowerCase();
    }
}
