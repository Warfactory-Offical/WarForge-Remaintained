package com.flansmod.warforge.api;


import com.flansmod.warforge.common.network.PacketBase;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Objects;

public class Vein {
    public final String translation_key;

    @Nonnull
    public final ResourceLocation[] component_ids;  // if this is null, then nothing is produced

    @Nonnull
    public final int[] component_yields;

    @Nonnull
    private Int2IntOpenHashMap dim_weights;  // weight out of 10,000 - inherently stores valid dim info

    @Nonnull
    public final int[] component_weights;

    public static int WEIGHT_FRACTION_DIGIT_COUNT = 4;

    // Packet Data:
    final private String VEIN_ENTRY;  // used to send over network on join
    final int ID;

    private static int idCounter = 0;

    public static int getNextID() { return idCounter++; }

    // we assume the values are in order as described by the other constructor
    // translation_key, {mc:comp1, mc:comp2}, {-1, 0, 1}, {0.5, 0.5, 1}, {0.4525, 0.5475}
    public Vein(final String vein_entry, int id) {
        VEIN_ENTRY = vein_entry;
        ID = idCounter++;

        // splits on all spaces/ commas in any combination not within curly brackets
        // looks ahead to see if either there are any number of {} combinations, or if there are no }
        // this should return the arguments as string in order
        String[] values = vein_entry.split("[((\\s*,\\s*)\\s*)](?=([^{}]*\\{[^{}]*}[^{}]*)|(?!.*}))");

        this.translation_key = values[0];

        // extract component yield and id data
        String[] component_id_strings = values[1].substring(1, values[1].length() - 1).split("[((\\s*,\\s*)\\s*)]");
        this.component_yields = new int[component_id_strings.length];
        this.component_ids = new ResourceLocation[component_id_strings.length];
        for (int i = 0; i < component_id_strings.length; ++i) {
            int yield_amount_end_index = component_id_strings[i].indexOf('#');
            this.component_yields[i] = Integer.decode(component_id_strings[i].substring(0, yield_amount_end_index));
            this.component_ids[i] = new ResourceLocation(component_id_strings[i].substring(yield_amount_end_index + 1));
        }

        // get the dimension information
        String[] dim_strings = values[2].substring(1, values[2].length() - 1).split("[((\\s*,\\s*)\\s*)]");
        int[] valid_dims = new int[dim_strings.length];
        for (int i = 0; i < dim_strings.length; ++i) {
            valid_dims[i] = Integer.decode(dim_strings[i]);
        }

        // get the dimension weights
        String[] dim_weight_strings = values[3].substring(1, values[3].length() - 1).split("[((\\s*,\\s*)\\s*)]");
        this.dim_weights = new Int2IntOpenHashMap(valid_dims.length);
        this.dim_weights.defaultReturnValue(0);  // by default, any dim not mentioned here results in 0 weight
        for (int i = 0; i < dim_weight_strings.length; ++i) {
            dim_weights.put(valid_dims[i], percentToInt(dim_weight_strings[i], WEIGHT_FRACTION_DIGIT_COUNT));
        }

        // get the component weights
        String[] comp_weight_strings = values[4].substring(1, values[4].length() - 1).split("[((\\s*,\\s*)\\s*)]");
        this.component_weights = new int[comp_weight_strings.length];
        for (int i = 0; i < comp_weight_strings.length; ++i) {
            component_weights[i] = percentToInt(comp_weight_strings[i], WEIGHT_FRACTION_DIGIT_COUNT);
        }
    }

    // takes a float as a string and converts it to an integer, using digits as the number of fractional digits to use
    public static int percentToInt(String float_string, int digits) {
        int result = 0;
        int decimal_position = float_string.indexOf('.');
        String whole_portion;
        String frac_portion;

        // handle case with no decimal
        if (decimal_position < 0) {
            whole_portion = float_string;
            frac_portion = "";
        } else {
            whole_portion = float_string.substring(0, decimal_position);
            frac_portion = float_string.substring(decimal_position + 1);
        }

        if (frac_portion.length() > digits) { frac_portion = frac_portion.substring(0, digits); }
        else {
            StringBuilder fractional = new StringBuilder(frac_portion);
            fractional.append('0');
            while (fractional.length() < digits) { fractional.append('0'); }
            frac_portion = fractional.toString();
        }

        int whole = Integer.decode(whole_portion);
        for (int i = 0; i < digits; ++i) { whole *= 10; }
        return whole + Integer.decode(frac_portion);
    }

    // creates a null vein
    public Vein(int dim, int weight) {
        VEIN_ENTRY = "";
        ID = idCounter++;
        this.translation_key = null;
        this.component_ids = new ResourceLocation[0];
        this.component_yields = new int[0];
        this.dim_weights = new Int2IntOpenHashMap(1);
        dim_weights.defaultReturnValue(0);
        dim_weights.put(dim, weight);
        this.component_weights = new int[0];
    }

    public int getDimWeight(int dim) { return dim_weights.get(dim); }
    public IntSet getValidDims() { return dim_weights.keySet(); }
    public int getID() { return ID; }

    public void setDimWeights(int dim, int weight) { dim_weights.put(dim, weight); }

    public boolean isNullVein() { return VEIN_ENTRY.equals(""); }

    void writeToBuf(ByteBuf buf) {
        PacketBase.writeUTF(buf, VEIN_ENTRY);
    }
}
