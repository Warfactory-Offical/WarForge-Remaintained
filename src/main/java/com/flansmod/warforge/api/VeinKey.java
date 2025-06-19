package com.flansmod.warforge.api;

import akka.japi.Pair;
import com.flansmod.warforge.common.DimChunkPos;
import com.flansmod.warforge.common.VeinConfigHandler;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import static com.flansmod.warforge.common.VeinConfigHandler.*;
import static com.flansmod.warforge.common.WarForgeMod.LOGGER;
import static com.flansmod.warforge.common.WarForgeMod.VEIN_MAP;

// used to more efficiently handle lots of veins
// random number generated from 1-10000 will turn into a key with that # as both bounds
public class VeinKey implements Comparable<VeinKey> {
    // inclusive
    private int lower_bound;

    // inclusive
    private int upper_bound;

    static int lower_bound_start = 0;

    // will always produce bounds on the range [0, 9999]
    public VeinKey(int weight) {
        this.lower_bound = lower_bound_start;
        this.upper_bound = lower_bound_start + weight - 1;
        lower_bound_start = (upper_bound + 1) % 10000;
    }

    // useful because it will be equal to whatever bounds it is inside of
    public VeinKey(int num, boolean unused) {
        this.lower_bound = num;
        this.upper_bound = num;
    }

    public int getLowerBound() { return lower_bound; }
    public int getUpperBound() { return upper_bound; }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) { return true; }
        if (!(obj instanceof VeinKey)) { return false; }

        // the keys are equal if one's range contains the other
        VeinKey key = (VeinKey) obj;
        return (key.getLowerBound() >= lower_bound && key.getUpperBound() <= upper_bound) || (
                key.getLowerBound() <= lower_bound && key.getUpperBound() >= upper_bound);
    }

    @Override
    public int compareTo(VeinKey key) {
        if (this.equals(key)) { return 0; }
        // we go off of the start of the key bounds to determine relative ordering
        if (key.getLowerBound() < lower_bound) { return 1; }
        return -1; // if lower bound is not less than, and they are not equal, then upper bound must be greater
    }

    public static void populateDimVeinMap(TreeMap<VeinKey, Vein> vein_map, int dim, ArrayList<Vein> all_veins) {
        // for later sorting
        ArrayList<Vein> smallest_weights = new ArrayList<>();
        ArrayList<Vein> medium_weights = new ArrayList<>();
        ArrayList<Vein> larger_weights = new ArrayList<>();
        ArrayList<Vein> largest_weights = new ArrayList<>();

        // for ease of use
        ArrayList<ArrayList<Vein>> sorted_veins = new ArrayList<>();
        sorted_veins.add(smallest_weights);
        sorted_veins.add(medium_weights);
        sorted_veins.add(larger_weights);
        sorted_veins.add(largest_weights);
        int remaining_weight = 10000;

        // attempt to categorize and store all veins
        for (Vein curr_vein : all_veins) {
            if (curr_vein.getDimWeight(dim) == 0) { continue; }  // if the vein has no weight in this dim, ignore it
            // check if we have exceeded the weight provided
            if (remaining_weight < curr_vein.getDimWeight(dim)) {
                LOGGER.atError().log("The maximum weight (10000) has been exceeded at vein " + curr_vein.translation_key
                        + "; ensure the sum of all vein fractional weights is <= 1.");
                curr_vein.setDimWeights(dim, remaining_weight);
                remaining_weight = 0;
            } else {
                remaining_weight -= curr_vein.getDimWeight(dim);
            }

            // sort into rough categories based on likelihood
            if (curr_vein.getDimWeight(dim) <= 1250) { smallest_weights.add(curr_vein); }
            else if (curr_vein.getDimWeight(dim) <= 2500) { medium_weights.add(curr_vein); }
            else if (curr_vein.getDimWeight(dim) <= 5000) { larger_weights.add(curr_vein); }
            else { largest_weights.add(curr_vein); }

            if (remaining_weight == 0) { break; }  // if there is no more weight to divy up, stop looping
        }

        // insert a null vein corresponding to remaining weight
        if (remaining_weight > 0) {
            // sort into rough categories based on likelihood
            if (remaining_weight <= 1250) { smallest_weights.add(new Vein(dim, remaining_weight)); }
            else if (remaining_weight <= 2500) { medium_weights.add(new Vein(dim, remaining_weight)); }
            else if (remaining_weight <= 5000) { larger_weights.add(new Vein(dim, remaining_weight)); }
            else { largest_weights.add(new Vein(dim, remaining_weight)); }
        }

        // sort based on weight in ascending order
        Comparator<Vein> weight_sorter = Comparator.comparingInt(vein -> vein.getDimWeight(dim));
        smallest_weights.sort(weight_sorter);
        medium_weights.sort(weight_sorter);
        larger_weights.sort(weight_sorter);
        largest_weights.sort(weight_sorter);

        // we will assign key bounds such that the least likely veins end up with the most extreme bounds
        // while the most likely veins have the intermediate bounds
        // this should hopefully bias the most likely veins towards the top of whatever map Java uses
        for (int i = 0; i < sorted_veins.size(); ++i) {
            ArrayList<Vein> curr_vein_category = sorted_veins.get(i);
            int half_point = (curr_vein_category.size() + 1) >> 1;
            for (int j = 0; j < half_point; ++j) {
                vein_map.put(new VeinKey(curr_vein_category.get(j).getDimWeight(dim)), curr_vein_category.get(j));
            }
        }

        // insert from largest to smallest
        for (int i = sorted_veins.size() - 1; i >= 0; --i) {
            ArrayList<Vein> curr_vein_category = sorted_veins.get(i);
            int half_point = (curr_vein_category.size() + 1) >> 1;
            for (int j = half_point; j < curr_vein_category.size(); ++j) {
                vein_map.put(new VeinKey(curr_vein_category.get(j).getDimWeight(dim)), curr_vein_category.get(j));
            }
        }
    }

    public static void populateVeinMap(Int2ObjectOpenHashMap<TreeMap<VeinKey, Vein>> vein_map, String[] vein_entries) {
        ArrayList<Vein> all_veins = new ArrayList<>(vein_entries.length);
        Int2IntOpenHashMap all_dims = new Int2IntOpenHashMap();
        all_dims.defaultReturnValue(0);
        for (String entry : vein_entries) {
            Vein curr_vein = new Vein(entry);  // store all veins provided for later use
            all_veins.add(curr_vein);

            // determine the dims this vein is present in
            for (int dim : curr_vein.getValidDims()) {
                all_dims.put(dim, all_dims.get(dim) + 1);  // increment the number of veins this dim has (unused)
            }
        }

        for (int dim : all_dims.keySet()) {
            vein_map.put(dim, new TreeMap<>());
            populateDimVeinMap(vein_map.get(dim), dim, all_veins);
        }
    }

    public static void populateVeinMap(Int2ObjectOpenHashMap<TreeMap<VeinKey, Vein>> vein_map, List<VeinEntry> vein_entries) {
        ArrayList<Vein> all_veins = new ArrayList<>(vein_entries.size());
        Int2IntOpenHashMap all_dims = new Int2IntOpenHashMap();
        all_dims.defaultReturnValue(0);
        for (VeinEntry entry : vein_entries) {
            Vein curr_vein = new Vein(entry);  // store all veins provided for later use
            all_veins.add(curr_vein);

            // determine the dims this vein is present in
            for (int dim : curr_vein.getValidDims()) {
                all_dims.put(dim, all_dims.get(dim) + 1);  // increment the number of veins this dim has (unused)
            }
        }

        for (int dim : all_dims.keySet()) {
            vein_map.put(dim, new TreeMap<>());
            populateDimVeinMap(vein_map.get(dim), dim, all_veins);
        }
    }


    public static int[] generateChunkHash(int chunk_x, int chunk_z, long seed) {
        // "hash" to determine the vein for this chunk
        int hash = (int) (seed * 2654435761L);
        hash = (int) ((hash + chunk_x) * 2654435761L);
        hash = (int) ((hash + chunk_z) * 2654435761L);
        hash = (hash << 1) >>> 1;  // ensure non-negative

        int quality = hash % 3;  // quality factor possibilities of poor, fair, rich
        hash %= 10000;

        return new int[]{hash, quality};
    }


    public static Pair<Vein, Quality> getVein(int dim, int chunkX, int chunkZ, long seed) {
        int[] chunk_hash = generateChunkHash(chunkX, chunkZ, seed);

        // if no veins exist for this dim, or at all, we just return null
        try {
            return new Pair<>(VEIN_MAP.get(dim).get(new VeinKey(chunk_hash[0], true)), Quality.getQuality(chunk_hash[1]));
        } catch (Exception exception) {
            return null;
        }
    }

    public static Pair<Vein, Quality> getVein(DimChunkPos dimChunkPos, long seed) {
        return getVein(dimChunkPos.mDim, dimChunkPos.x, dimChunkPos.z, seed);
    }
}
