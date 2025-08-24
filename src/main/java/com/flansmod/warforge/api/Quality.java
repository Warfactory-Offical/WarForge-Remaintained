package com.flansmod.warforge.api;

public enum Quality {

    RICH,
    FAIR,
    POOR;

    static Quality getQuality(int qualityIndex) {
        switch (qualityIndex) {
            case 1:
                return FAIR;
            case 2:
                return RICH;
            default:
                return POOR;
        }
    }

    public String getTranslationKey() {
        return "warforge.info.vein." + this.toString().toLowerCase();
    }
}
