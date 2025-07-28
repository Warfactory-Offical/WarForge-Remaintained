package com.flansmod.warforge.api.vein;

import com.flansmod.warforge.api.vein.init.VeinUtils;

// used to more efficiently handle lots of veins
// random number generated from 1-10000 will turn into a key with that # as both bounds
public class VeinKey implements Comparable<VeinKey> {
    // inclusive
    private short lowerBound;

    // inclusive
    private short upperBound;

    private static short lowerBoundStart = 0;

    public static final VeinKey NULL_KEY = new VeinKey(VeinUtils.NULL_VEIN_ID);

    // will always produce bounds on the range [0, 9999]
    public VeinKey(short weight, boolean unusedStoredKeyIndicator) {
        this.lowerBound = lowerBoundStart;
        this.upperBound = (short) (lowerBoundStart + weight - 1);
        lowerBoundStart = (short) ((upperBound + 1) % 10000);
    }

    // useful because it will be equal to whatever bounds it is inside of
    public VeinKey(short weight) {
        this.lowerBound = weight;
        this.upperBound = weight;
    }

    public void rebaseKey(short weight) {
        this.lowerBound = weight;
        this.upperBound = weight;
    }

    public int getLowerBound() { return lowerBound; }
    public int getUpperBound() { return upperBound; }

    public int calcWeight() { return upperBound - lowerBound + 1; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) { return true; }
        if (!(obj instanceof VeinKey key)) { return false; }

        // the keys are equal if one's range contains the other
        return (key.getLowerBound() >= lowerBound && key.getUpperBound() <= upperBound) || (
                key.getLowerBound() <= lowerBound && key.getUpperBound() >= upperBound);
    }

    @Override
    public int compareTo(VeinKey key) {
        if (this.equals(key)) { return 0; }
        // we go off of the start of the key bounds to determine relative ordering
        if (key.getLowerBound() < lowerBound) { return 1; }
        return -1; // if lower bound is not less than, and they are not equal, then upper bound must be greater
    }

    public static void resetBoundStart() { lowerBoundStart = 0; }
}
