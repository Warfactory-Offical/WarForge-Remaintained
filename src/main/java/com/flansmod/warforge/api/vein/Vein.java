package com.flansmod.warforge.api.vein;


import com.flansmod.warforge.api.vein.init.VeinConfigHandler;
import com.flansmod.warforge.api.vein.init.VeinUtils;
import com.flansmod.warforge.common.WarForgeMod;
import com.flansmod.warforge.server.StackComparable;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import static com.flansmod.warforge.api.vein.init.VeinUtils.percentToShort;
import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;

public class Vein {
    public final String translationKey;
    @Nonnull
    public final LinkedHashSet<StackComparable> compIds;  // if this is null, then nothing is produced
    @Nonnull
    private final Int2ObjectOpenHashMap<short[]> dimWeights;  // weight out of 10,000 and expected weight - inherently stores valid dim info
    @Nonnull
    public final Object2ObjectOpenHashMap<StackComparable, ArrayList<Int2ShortOpenHashMap>> compWeights;
    @Nonnull
    public final Object2ObjectOpenHashMap<StackComparable, ArrayList<Int2FloatOpenHashMap>> compYields;  // compId -> dimId -> yield as Float (multiplier applied already)

    // Packet Data:
    public final ByteBuf SERIALIZED_ENTRY;
    private final short id;

    public Vein(final VeinConfigHandler.VeinEntry veinEntry) {
        if (FMLCommonHandler.instance().getSide() == Side.SERVER) { SERIALIZED_ENTRY = veinEntry.serialize(); }  // for transmission over network
        else { SERIALIZED_ENTRY = null; }  // we don't expect the clients to send the vein back over net

        // carry over the obvious values
        this.translationKey = veinEntry.translationKey;
        id = veinEntry.id;
        dimWeights = new Int2ObjectOpenHashMap<>(veinEntry.dimWeights.size());
        dimWeights.defaultReturnValue(new short[]{0, 0});

        // carry over the dimension weight information
        for(VeinConfigHandler.DimWeight dimWeight : veinEntry.dimWeights.values()) {
            dimWeights.put(dimWeight.id, new short[]{ percentToShort(dimWeight.weight),
                    (short) (VEIN_HANDLER.megachunkArea * dimWeight.weight)});
        }

        // integrate multipliers into yields
        this.compIds = new LinkedHashSet<>(veinEntry.components.size());
        this.compWeights = new Object2ObjectOpenHashMap<>(veinEntry.components.size());
        this.compYields = new Object2ObjectOpenHashMap<>(veinEntry.components.size());
        this.compYields.defaultReturnValue(null);
        for(VeinConfigHandler.Component component : veinEntry.components){
            // we want to map each stack comparable component to some list of weights and yields
            // a component's item may be repeated to give different chances to get a range of values
            // for a set of components with equivalent items, the stack comparable of the first is preferred and stored
            var stackComparable = new StackComparable(component.item);
            if (!compIds.contains(stackComparable)) {
                // if we haven't seen this item before, then add it to the relevant locations
                compIds.add(stackComparable);
                compWeights.put(stackComparable, new ArrayList<>(1));
                compYields.put(stackComparable, new ArrayList<>(1));
            }

            // apply default weight
            component.weights.defaultReturnValue((short) 10000);  // for any dimensions not present
            compWeights.get(stackComparable).add(component.weights);  // store the weights

            // apply multiplier to default yield value to get dimYield value
            Int2FloatOpenHashMap yields = new Int2FloatOpenHashMap();
            yields.defaultReturnValue(component.yield);  // default is yield * default mult [1.0]

            // apply the component level dim multipliers
            if (component.multipliers != null) {
                for (var entry : component.multipliers.int2FloatEntrySet()) {
                    // for some reason cast to int is needed to disambiguate the call to put
                    yields.put(entry.getIntKey(), component.yield * entry.getFloatValue() / VeinUtils.WEIGHT_FRACTION_TENS_POW);
                }
            }

            // apply the vein level dim multipliers for any not already present
            for (var entry : veinEntry.dimWeights.int2ObjectEntrySet()) {
                if (yields.containsKey(entry.getIntKey())) { continue; }
                yields.put(entry.getIntKey(), component.yield * entry.getValue().multiplier / VeinUtils.WEIGHT_FRACTION_TENS_POW);
            }

            compYields.get(stackComparable).add(yields);  // store the yields
        }

        VEIN_HANDLER.ID_TO_VEINS.put(id, this);
    }

    public short getDimWeight(int dim) { return dimWeights.get(dim)[0]; }
    public short getDimExpectedCount(int dim) { return dimWeights.get(dim)[1]; }
    public IntSet getValidDims() { return dimWeights.keySet(); }
    public short getId() { return id; }

    public String toString() {
        String dimIds = this.dimWeights.keySet().stream()
                .map(dimID -> Integer.toString(dimID))
                .collect(Collectors.joining(", ", "{", "}"));

        String dimWeights = this.dimWeights.values().stream()
                .map(dw -> String.format("%.4f", ((float) dw[0]) / 10000) + "; EXP: " + dw[1])
                .collect(Collectors.joining(", ", "{", "}"));

        String components = this.compIds.stream()
                .map(compId -> compId.toString() + ": [" + compWeights.get(compId).toString() + "; " + compYields.get(compId).toString() + "]")
                .collect(Collectors.joining(", ", "{", "}"));

        return "ID: " + getId() + "DATA: " + String.join(", ", translationKey, dimIds, dimWeights, "COMP DATA: \n", components);
    }
}
