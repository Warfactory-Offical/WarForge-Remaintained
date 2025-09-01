package com.flansmod.warforge.api.vein.init;

import akka.japi.Pair;
import com.flansmod.warforge.api.vein.Quality;
import com.flansmod.warforge.api.vein.Vein;
import com.flansmod.warforge.api.vein.VeinKey;
import com.flansmod.warforge.server.StackComparable;
import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ShortAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortRBTreeMap;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.Side;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.flansmod.warforge.client.ClientProxy.VEIN_ENTRIES;
import static com.flansmod.warforge.common.CommonProxy.YIELD_QUALITY_MULTIPLIER;
import static com.flansmod.warforge.common.WarForgeMod.LOGGER;
import static com.flansmod.warforge.common.WarForgeMod.VEIN_HANDLER;

public class VeinUtils {
    public static final short WEIGHT_FRACTION_TENS_POW = 10000;  // should stay 10,000 so that it fits within a short
    public final static short NULL_VEIN_ID = (short) 0x00_00_FF_FF;  // # of distinct qualities limited to 7

    public Short2ObjectOpenHashMap<Vein> ID_TO_VEINS = new Short2ObjectOpenHashMap<>();

    // we pack the chunk indices as x(higher bits) z(lower bits) and vein data as iteration id (higher bits) vein id (lower bits)
    // for the mega chunk occurrence data, the default return value set's first entry is the current weight
    private final Int2ObjectOpenHashMap<Object2ShortAVLTreeMap<VeinKey>> DIM_VEIN_WEIGHT_MAP;  // dim -> [key->id map, .obj -> veinkey -> id
    private final Int2ObjectOpenHashMap<Long2ObjectOpenHashMap<Pair<Short2ShortOpenHashMap, Short2ShortRBTreeMap>>> MEGA_CHUNK_OCCURRENCE_DATA;  // dim -> megchunk -> (id -> occurrences and short [byte-X, byte-Z] offsets -> id)

    public final short iterationId;
    public final short megachunkLength;
    public final short megachunkArea;

    public boolean hasFinishedInit;

    protected VeinUtils(short iterationId, short megachunkLength) {
        this.iterationId = iterationId;
        this.megachunkLength = megachunkLength;
        this.megachunkArea = (short) (megachunkLength * megachunkLength);
        ID_TO_VEINS.defaultReturnValue(null);

        DIM_VEIN_WEIGHT_MAP = new Int2ObjectOpenHashMap<>();
        MEGA_CHUNK_OCCURRENCE_DATA = new Int2ObjectOpenHashMap<>();
        hasFinishedInit = false;
    }

    public static short percentToShort(float percent) {
        percent *= VeinUtils.WEIGHT_FRACTION_TENS_POW;
        return (short) Math.round(percent);
    }

    // returns a formatted percent string, dropping the decimal part if it is 0; assumes 4 sig figs
    public static String shortToPercentStr(short percent) {
        int whole = percent / 100;
        int decimal = percent - whole * 100;
        String decimalStr = decimal > 0 ? "." + decimal + "%" : "%";
        return whole + decimalStr;
    }

    // return each identical item component's weight, guaranteed yield and percent yield amount, respectively
    // assumes comp and dim are both present and does not consider component weight
    public static ArrayList<short[]> getYieldInfo(StackComparable comp, Pair<Vein, Quality> veinInfo, int dim) {
        Vein vein = veinInfo.first();
        Quality qual = veinInfo.second();
        ArrayList<Int2FloatOpenHashMap> yields = vein.compYields.get(comp);  // comp -> LIST OF dim : yield
        ArrayList<Int2ShortOpenHashMap> weights = vein.compWeights.get(comp);
        assert yields.size() == weights.size();  // sanity check
        ArrayList<short[]> yieldInfos = new ArrayList<>(yields.size());  // result

        // for each sub-component (same item, different stats), store its weight, guaranteed yield, and percent yield
        for (int subCompIndex = 0; subCompIndex < yields.size(); ++subCompIndex) {
            float yield = yields.get(subCompIndex).get(dim);

            // scale the guaranteed yield based on quality
            yield *= qual.getLocalMultiplier(vein);

            short[] result = new short[]{
                    weights.get(subCompIndex).get(dim),
                    (short) yield,
                    0
            };

            result[2] = percentToShort(yield - result[1]);

            yieldInfos.add(result);
        }

        return yieldInfos;
    }

    public boolean dimHasVeins(int dim) {
        return DIM_VEIN_WEIGHT_MAP.containsKey(dim);
    }

    // gets weight without checking for existence of key
    public short getVeinId(int dim, VeinKey key) {
        return DIM_VEIN_WEIGHT_MAP.get(dim).getShort(key);
    }

    // gets vein id and ensures if no veins exists in dimension that no error occurs
    public short getVeinIdSafe(int dim, VeinKey key) {
        if (!dimHasVeins(dim)) { return NULL_VEIN_ID; }
        return getVeinId(dim, key);
    }

    // gets the vein occurrences without performing any checks
    public short getVeinOccurrences(int dim, long megachunkKey, short veinId) {
        return MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey).first().get(veinId);
    }

    // gets the vein occurrences and performs any necessary setup
    public short getVeinOccurrencesSafe(int dim, long megachunkKey, short veinId) {
        ensureMegachunkPopulated(dim, megachunkKey);
        return getVeinOccurrences(dim, megachunkKey, veinId);
    }

    // because this is only based on coordinate and seed, we don't need to store data we get from this which doesn't change
    public int[] generateChunkHash(int chunk_x, int chunk_z, long seed) {
        // "hash" to determine the vein for this chunk
        int hash = (int) (seed * 2654435761L);
        hash = (int) ((hash + chunk_x) * 2654435761L);
        hash = (int) ((hash + chunk_z) * 2654435761L);
        hash = (hash << 1) >>> 1;  // ensure non-negative

        int quality = hash % Quality.values().length;  // quality factor possibilities of poor, fair, rich
        //hash %= 10000;  we hash it later, for now we just want the raw value

        return new int[]{hash, quality};
    }

    public long produceMegachunkKey(int chunkX, int chunkZ) {
        // we shift by one less than the megachunkLength to ensure that SE is 0-0, NE is 0-[-1], SW is -1-0, etc
        long megachunkKey = (chunkX - megachunkLength + 1) / megachunkLength;
        megachunkKey = megachunkKey << 32;  // shift X into higher bits
        megachunkKey += (chunkZ - megachunkLength + 1) / megachunkLength;
        return megachunkKey;
    }

    // we store the chunk offset as offsetx-offsetz, with each being the absolute
    // offset from the smallest magnitude chunk coordinates within a given megachunk
    // because we limit the megachunk length to 180, we still need 8 bits to store offset information
    public short produceChunkOffset(int chunkX, int chunkZ) {
        // for negative indices, their actual chunk remainder starts at -1, which should be 0
        if (chunkX < 0) { ++chunkX; }
        if (chunkZ < 0) { ++chunkZ; }
        short offset = (short) (Math.abs(chunkX) % megachunkLength << 8);
        offset += (short) (Math.abs(chunkZ) % megachunkLength);
        return offset;
    }

    // returns the chunkX and chunkZ coordinates in an int array
    public int[] recoverChunkCoords(short offset, short megachunkLength, long megachunkKey) {
        int megachunkX = (int) (megachunkKey >>> 32);
        int megachunkZ = (int) megachunkKey;

        int chunkX = offset >>> 8;
        int chunkZ = offset & 255;

        // -1 megachunk coords start at the origin, not having traveled a megachunk length
        // individual chunk coords in the negative direction will have an offset one lower than their coord
        if (megachunkX < 0) {
            ++megachunkX;
            --chunkX;
        }
        if (megachunkZ < 0) {
            ++megachunkZ;
            --chunkZ;
        }

        chunkX += megachunkX * megachunkLength;
        chunkZ += megachunkZ * megachunkLength;

        return new int[]{chunkX, chunkZ};
    }

    public Vein getVein(short veinId) {
        return ID_TO_VEINS.get(veinId);
    }

    // returns null to indicate null vein
    public Pair<Vein, Quality> getVein(int dim, int chunkX, int chunkZ, long seed) {
        long megachunkKey = produceMegachunkKey(chunkX, chunkZ);
        short offset = produceChunkOffset(chunkX, chunkZ);

        if (!dimHasVeins(dim)) { return null; }  // we don't need to store any data for empty dimensions

        // check if the world has our data
        Pair<Vein, Quality> veinInfo = pullVein(dim, megachunkKey, offset);
        if (veinInfo != null) { return veinInfo.first() == null ? null : veinInfo; }

        // generate the vein info and extract the veinId and quality
        veinInfo = generateVeinInfo(dim, megachunkKey, chunkX, chunkZ, seed);
        short veinId = veinInfo == null ? NULL_VEIN_ID : veinInfo.first().getId();
        int quality = veinInfo == null ? 7 : veinInfo.second().ordinal();

        // add the vein info to storage and then return it
        addVeinInfo(dim, megachunkKey, offset, veinId, quality);
        return veinInfo;
    }

    public boolean isMegachunkPopulated(int dim, long megachunkKey) {
        return MEGA_CHUNK_OCCURRENCE_DATA.containsKey(dim) && MEGA_CHUNK_OCCURRENCE_DATA.get(dim).containsKey(megachunkKey);
    }

    public void ensureMegachunkPopulated(int dim, long megachunkKey) {
        if (!isMegachunkPopulated(dim, megachunkKey) && DIM_VEIN_WEIGHT_MAP.containsKey(dim)) {
            populateMegachunkInfo(dim, megachunkKey);
        }
    }

    // should only be called for dimensions which actually expect to see veins within them
    public void populateMegachunkInfo(int dim, long megachunkKey) {
        // if the dimension has never been initialized, but should have weights, then initialize it
        MEGA_CHUNK_OCCURRENCE_DATA.put(dim, new Long2ObjectOpenHashMap<>());
        var currDimMegachunks = MEGA_CHUNK_OCCURRENCE_DATA.get(dim);
        currDimMegachunks.put(megachunkKey, new Pair<>(
            new Short2ShortOpenHashMap(),
            new Short2ShortRBTreeMap()
        ));

        // get a reference to the current megachunk and intialize it
        var currMegachunk = currDimMegachunks.get(megachunkKey);
        for (var entry : DIM_VEIN_WEIGHT_MAP.get(dim).object2ShortEntrySet()) {
            if (entry.getKey() == VeinKey.NULL_KEY) { continue; }  // ignore the null key
            currMegachunk.first().put(entry.getShortValue(), (short) 0);
        }

        // first map stores id -> occurrences map, second stores offset -> id map
        currMegachunk.first().defaultReturnValue(WEIGHT_FRACTION_TENS_POW);  // first has default rv of weight left
        currMegachunk.second().defaultReturnValue(NULL_VEIN_ID);  // second just indicates no such coordinate is present
    }

    // decompresses the vein info, with separate handling for server and client, returning null for the null vein and
    // a pair of null values for an unrecognized vein on the client
    public static Pair<Vein, Quality> decompressVeinInfo(short veinInfo) {
        if (veinInfo == NULL_VEIN_ID) { return null; }

        short[] decompVeinInfo = splitVeinInfo(veinInfo);
        if (FMLCommonHandler.instance().getEffectiveSide() == Side.SERVER) {
            return new Pair<>(VEIN_HANDLER.ID_TO_VEINS.get(decompVeinInfo[0]), Quality.getQuality(decompVeinInfo[1]));
        } else {
            Vein targetVein = VEIN_ENTRIES.get(decompVeinInfo[0]);
            if (targetVein == null) { return new Pair<>(null, null); }  // use this to indicate we don't know the vein
            return new Pair<>(targetVein, Quality.getQuality(decompVeinInfo[1]));
        }
    }

    public static short[] splitVeinInfo(short veinInfo) {
        if (veinInfo == NULL_VEIN_ID) { return new short[]{NULL_VEIN_ID, (short) 7}; }  // null vein id is also its info
        return new short[]{(short) (veinInfo & 0x00_00_1F_FF), (short) ((veinInfo & 0x00_00_E0_00) >> 13)};
    }

    public short compressVeinInfo(int veinId, int qualityIndex) {
        return (short) (veinId | (qualityIndex << 13));
    }

    public short compressVeinInfo(Pair<Vein, Quality> veinInfo) {
        if (veinInfo == null) { return NULL_VEIN_ID; }
        return compressVeinInfo(veinInfo.first().getId(), veinInfo.second().ordinal());
    }

    // does not assume megachunk is populated
    public Pair<Vein, Quality> pullVein(int dim, long megachunkKey, short offset) {
        // check if the vein exists
        if (!isMegachunkPopulated(dim, megachunkKey)) { return null; }

        var offsetIds = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey).second();
        if (!offsetIds.containsKey(offset)) { return null; }  // does logarithmic search; allows drv of null vein

        // the null vein does not correspond to any vein or quality, but does actually exist
        short chunkData = offsetIds.get(offset);
        if (chunkData == NULL_VEIN_ID) { return new Pair<>(null, null); }

        // format the data correctly and return the result
        short[] unpackedData = splitVeinInfo(chunkData);
        return new Pair<>(ID_TO_VEINS.get(unpackedData[0]), Quality.getQuality(unpackedData[1]));
    }

    // returns the vein info for the current chunk, with null being returned for "no vein"
    public Pair<Vein, Quality> generateVeinInfo(int dim, long megachunkKey, int chunkX, int chunkZ, long seed) {
        if (!DIM_VEIN_WEIGHT_MAP.containsKey(dim)) { return null; }  // if we cannot generate any chunks for this dim, return null
        ensureMegachunkPopulated(dim, megachunkKey);

        int[] chunkHash = generateChunkHash(chunkX, chunkZ, seed);
        Object2ShortAVLTreeMap<VeinKey> currDimWeights = DIM_VEIN_WEIGHT_MAP.get(dim);
        var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);

        // we limit the hash result based on the remaining weight in the megachunk, then skip over veins which exceed
        // their expected value [floor(megachunkLength^2 * weight / 1000)]
        short weightRemaining = currMegachunk.first().defaultReturnValue();
        boolean doRandomRoll = weightRemaining == 0;

        // get the vein id from the hash by converting it into a vein key
        short trimmedHash = (short) (chunkHash[0] % weightRemaining);
        VeinKey currVeinKey = new VeinKey(trimmedHash);
        short currID = currDimWeights.getShort(currVeinKey);

        // return null for the null vein, or proceed
        Vein currVein = ID_TO_VEINS.get(currID);
        short dimExpCount = getDimExpCount(currVein, dim);

        // if some vein has occurred too many times, we may need to skip over it if we select it
        if (!doRandomRoll && weightRemaining < WEIGHT_FRACTION_TENS_POW) {
            while (currMegachunk.first().get(currID) >= dimExpCount) {
                trimmedHash += getDimWeight(currVein, dim);
                currVeinKey.rebaseKey(trimmedHash);
                currID = currDimWeights.getShort(currVeinKey);
                currVein = ID_TO_VEINS.get(currID);
            }
        }

        if (currVein == null) { return null; }  // dont return a pair with null info
        return new Pair<>(currVein, Quality.getQuality(chunkHash[1]));
    }

    public short getDimWeight(Vein vein, int dim) {
        return vein == null ? DIM_VEIN_WEIGHT_MAP.get(dim).defaultReturnValue() : vein.getDimExpectedCount(dim);
    }

    public short getDimExpCount(Vein vein, int dim) {
        return vein == null ? DIM_VEIN_WEIGHT_MAP.get(dim).getShort(VeinKey.NULL_KEY) : vein.getDimExpectedCount(dim);
    }

    public void addVeinInfo(int dim, int chunkX, int chunkZ, short veinId, int quality) {
        long megachunkKey = produceMegachunkKey(chunkX, chunkZ);
        short offset = produceChunkOffset(chunkX, chunkZ);

        // don't add vein info that we already have
        addVeinInfo(dim, megachunkKey, offset, veinId, quality);
    }

    // does check to ensure the targeted megachunk is populated; will accept a veinId of NULL VEIN ID
    public void addVeinInfo(int dim, long megachunkKey, short offset, short veinId, int quality) {
        // check the megachunk is present and collect it
        ensureMegachunkPopulated(dim, megachunkKey);
        var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);

        // we need to update the number of occurrences and adjust the remaining weight accordingly
        int currOccurrences = currMegachunk.first().get(veinId);
        currMegachunk.first().put(veinId, (short) (currOccurrences + 1));
        Vein currVein = ID_TO_VEINS.get(veinId);  // will be null for the null vein

        // we now need to place the offset position info
        short veinData;
        if (veinId == NULL_VEIN_ID) { veinData = NULL_VEIN_ID; }
        else { veinData = compressVeinInfo(veinId, quality); }
        currMegachunk.second().put(offset, veinData);

        // if all veins exceeded expected don't do any check
        if (currMegachunk.first().defaultReturnValue() == 0) { return; }

        short expectedCount = getDimExpCount(currVein, dim);

        // check if we need to update the weight - when remaining weight is 0 we are just picking randomly again
        if (currMegachunk.first().defaultReturnValue() > 0 && currOccurrences + 1 >= expectedCount) {
            int dimWeight = getDimWeight(currVein, dim);
            currMegachunk.first().defaultReturnValue((short) (currMegachunk.first().defaultReturnValue() - dimWeight));
        }
    }

    private void populateDimVeinMap(int dim) {
        // for later sorting
        ArrayList<Vein> smallestWeights = new ArrayList<>();
        ArrayList<Vein> mediumWeights = new ArrayList<>();
        ArrayList<Vein> largerWeights = new ArrayList<>();
        ArrayList<Vein> largestWeights = new ArrayList<>();

        // for ease of use
        ArrayList<ArrayList<Vein>> sortedVeins = new ArrayList<>();
        sortedVeins.add(smallestWeights);
        sortedVeins.add(mediumWeights);
        sortedVeins.add(largerWeights);
        sortedVeins.add(largestWeights);
        short remainingWeight = WEIGHT_FRACTION_TENS_POW;

        var dimWeightMap = DIM_VEIN_WEIGHT_MAP.get(dim);

        // attempt to categorize and store all veins
        for (Vein currVein : ID_TO_VEINS.values()) {
            short currDimWeight = currVein.getDimWeight(dim);
            if (currDimWeight == 0) { continue; }  // if the vein has no weight in this dim, ignore it
            // check if we have exceeded the weight provided
            if (remainingWeight < currDimWeight) {
                LOGGER.atError().log("The maximum weight (10000) has been exceeded at vein " + currVein.translationKey
                        + " in dim " + dim + "; ignoring current vein.");
                continue;
            } else {
                remainingWeight -= currDimWeight;
            }

            // sort into rough categories based on likelihood
            if (currDimWeight <= 1250) { smallestWeights.add(currVein); }
            else if (currDimWeight <= 2500) { mediumWeights.add(currVein); }
            else if (currDimWeight <= 5000) { largerWeights.add(currVein); }
            else { largestWeights.add(currVein); }
        }

        final short nullWeight = remainingWeight;

        // we need to add a key corresponding to the null vein weight left
        if (nullWeight > 0) {
            // sort into rough categories based on likelihood
            if (nullWeight <= 1250) { smallestWeights.add(null); }
            else if (nullWeight <= 2500) { mediumWeights.add(null); }
            else if (nullWeight <= 5000) { largerWeights.add(null); }
            else { largestWeights.add(null); }
        }

        // sort based on weight in ascending order
        Comparator<Vein> weight_sorter = (vein1, vein2) -> {
            short weight1 = vein1 == null ? nullWeight : vein1.getDimWeight(dim);
            short weight2 = vein2 == null ? nullWeight : vein2.getDimWeight(dim);

            return Short.compare(weight1, weight2);
        };

        smallestWeights.sort(weight_sorter);
        mediumWeights.sort(weight_sorter);
        largerWeights.sort(weight_sorter);
        largestWeights.sort(weight_sorter);

        // we will assign key bounds such that the least likely veins end up with the most extreme bounds
        // while the most likely veins have the intermediate bounds
        // this should hopefully bias the most likely veins towards the top of the tree
        for (int i = 0; i < sortedVeins.size(); ++i) {
            ArrayList<Vein> currVeinCategory = sortedVeins.get(i);
            int halfPoint = (currVeinCategory.size() + 1) >> 1;
            for (int j = 0; j < halfPoint; ++j) {
                Vein currVein = currVeinCategory.get(j);
                short currWeight = nullWeight;
                short currID = NULL_VEIN_ID;
                if (currVein != null) {
                    currWeight = currVein.getDimWeight(dim);
                    currID = currVein.getId();
                }

                dimWeightMap.put(new VeinKey(currWeight, false), currID);
            }
        }

        // insert from largest to smallest
        for (int i = sortedVeins.size() - 1; i >= 0; --i) {
            ArrayList<Vein> currVeinCategory = sortedVeins.get(i);
            int halfPoint = (currVeinCategory.size() + 1) >> 1;
            for (int j = halfPoint; j < currVeinCategory.size(); ++j) {
                // get the vein and its details; we may insert a null vein so we need to check
                Vein currVein = currVeinCategory.get(j);
                short currWeight = nullWeight;
                short currID = NULL_VEIN_ID;
                if (currVein != null) {
                    currWeight = currVein.getDimWeight(dim);
                    currID = currVein.getId();
                }

                dimWeightMap.put(new VeinKey(currWeight, false), currID);
            }
        }

        // we need to store data about the null vein
        // default return value is the null vein weight, while the null key returns the expected null vein occurrences
        dimWeightMap.defaultReturnValue(nullWeight);
        dimWeightMap.put(VeinKey.NULL_KEY, (short) (VEIN_HANDLER.megachunkArea * nullWeight / VeinUtils.WEIGHT_FRACTION_TENS_POW));
    }

    protected void populateVeinMap(@Nullable List<VeinConfigHandler.VeinEntry> veinEntries) {
        if (veinEntries == null || veinEntries.size() == 0) { return; }  // if we are passed null then we did not read any entries

        for (VeinConfigHandler.VeinEntry entry : veinEntries) {
            Vein currVein = new Vein(entry);  // store all veins provided for later use
            ID_TO_VEINS.put(currVein.getId(), currVein);

            // determine the dims this vein is present in
            for (int dim : currVein.getValidDims()) {
                DIM_VEIN_WEIGHT_MAP.put(dim, new Object2ShortAVLTreeMap<>());
            }
        }

        for (int dim : DIM_VEIN_WEIGHT_MAP.keySet()) {
            populateDimVeinMap(dim);
        }

        hasFinishedInit = true;
    }

    private String getVeinInfoID(short veinId) {
        return "vinfo_" + veinId;
    }
    private String getDimInfoID(int dim) { return "dinfo_" + dim; }

    public void readFromNBT(NBTTagCompound tags) {
        if (!tags.hasKey("vein_it_id") || tags.getShort("vein_it_id") != VEIN_HANDLER.iterationId) { return; }  // don't read data with mismatching iteration id
        NBTTagCompound dims = tags.getCompoundTag("vein_dims");

        var validDims = DIM_VEIN_WEIGHT_MAP.keySet();
        for (int dim : validDims) {
            NBTTagCompound currDim = dims.getCompoundTag(getDimInfoID(dim));
            if (currDim.isEmpty()) { continue; }  // if dim isn't present, we can't read its data

            // get all the current vein ID's for the current dimension
            final short[] veinIDs = DIM_VEIN_WEIGHT_MAP.get(dim).values().toShortArray();
            ArrayList<String> veinIDStrings = new ArrayList<>(veinIDs.length);
            for (short veinID : veinIDs) { veinIDStrings.add(getVeinInfoID(veinID)); }

            // read over each megachunk
            NBTTagList megachunks = currDim.getTagList("megachunks", 10);  // 10 corresponds to compound tags for tag lists
            for (int megachunkTagIndex = 0; megachunkTagIndex < megachunks.tagCount(); ++megachunkTagIndex) {
                // get megachunk nbt and general data
                NBTTagCompound currMegachunkNBT = megachunks.getCompoundTagAt(megachunkTagIndex);
                long megachunkKey = currMegachunkNBT.getLong("key");
                ensureMegachunkPopulated(dim, megachunkKey);

                var currMegachunk = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);

                // iterate over all veins by known ID's for the current dimension
                NBTTagCompound veins = currMegachunkNBT.getCompoundTag("veins");
                for (int veinIDIndex = 0; veinIDIndex < veinIDs.length; ++veinIDIndex) {
                    NBTTagList veinData = veins.getTagList(veinIDStrings.get(veinIDIndex), 7);
                    if (veinData.isEmpty()) { continue; }  // no saved data for a vein with this id

                    // iterate over all vein occurrences
                    for (int veinOccurrences = 0; veinOccurrences < veinData.tagCount(); ++veinOccurrences) {
                        // get the vein data and create the offset
                        byte[] rawVeinData = ((NBTTagByteArray) veinData.get(veinOccurrences)).getByteArray();
                        short offset = (short) (((int) rawVeinData[0]) << 8);
                        offset += rawVeinData[1];

                        // get the vein info to store; null veins don't have quality info
                        short veinInfo;
                        if (veinIDs[veinIDIndex] == NULL_VEIN_ID) { veinInfo = NULL_VEIN_ID; }
                        else { veinInfo = compressVeinInfo(veinIDs[veinIDIndex], rawVeinData[2]); }

                        // store relationship between offset and the veinInfo
                        currMegachunk.second().put(offset, veinInfo);
                    }

                    // store the number of occurrences for the given vein
                    short numOccurrences = (short) veinData.tagCount();
                    currMegachunk.first().put(veinIDs[veinIDIndex], numOccurrences);

                    // check if we exceeded the expected weight
                    Vein currVein = getVein(veinIDs[veinIDIndex]);
                    if (numOccurrences < getDimExpCount(currVein, dim)) { continue; }

                    // if we exceed the expected weight, we need to indicate that
                    short weightBeforeVein = currMegachunk.first().defaultReturnValue();
                    currMegachunk.first().defaultReturnValue((short) (weightBeforeVein - currVein.getDimWeight(dim)));
                }
            }
        }
    }

    public void WriteToNBT(NBTTagCompound tags) {
        tags.setShort("vein_it_id", VEIN_HANDLER.iterationId);
        NBTTagCompound dims = new NBTTagCompound();

        // iterate over each dimension with occurrence data
        var writtenDims = MEGA_CHUNK_OCCURRENCE_DATA.keySet();
        for (int dim : writtenDims) {
            NBTTagCompound currDim = new NBTTagCompound();

            NBTTagList megachunks = new NBTTagList();
            currDim.setTag("megachunks", megachunks);

            // get every chunk offset and corresponding vein qual+id short to store them (encodes occurrence data by # offsets)
            for (long megachunkKey : MEGA_CHUNK_OCCURRENCE_DATA.get(dim).keySet()) {
                var currMegachunkData = MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey);
                NBTTagCompound currMegachunkNBT = new NBTTagCompound();
                currMegachunkNBT.setLong("key", megachunkKey);

                NBTTagCompound veins = new NBTTagCompound();
                currMegachunkNBT.setTag("veins", veins);

                // for each occurrence of a vein, store it under the vein id
                for (var entry : MEGA_CHUNK_OCCURRENCE_DATA.get(dim).get(megachunkKey).second().short2ShortEntrySet()) {
                    // get id and qual information and make sure the tag for that vein exists
                    short[] idQual = splitVeinInfo(entry.getShortValue());
                    String veinTagID = getVeinInfoID(idQual[0]);
                    if (!veins.hasKey(veinTagID)) {
                        veins.setTag(veinTagID, new NBTTagList());
                    }

                    // for every occurrence, tie the offset and quality to the vein
                    byte offsetX = (byte) (entry.getShortKey() >>> 8);
                    byte offsetZ = (byte) (entry.getShortKey());
                    byte qual = (byte) (idQual[1]);

                    byte[] bytesToStore;
                    if (idQual[0] == NULL_VEIN_ID) { bytesToStore = new byte[]{offsetX, offsetZ}; }
                    else { bytesToStore = new byte[]{offsetX, offsetZ, qual}; }

                    // store the data
                    NBTTagList veinOffsetQualBEBytes = veins.getTagList(veinTagID, 7);  // 7 is for byte array
                    veinOffsetQualBEBytes.appendTag(new NBTTagByteArray(bytesToStore));
                }

                megachunks.appendTag(currMegachunkNBT);  // store the curr megachunk in the list of megachunks
            }

            dims.setTag(getDimInfoID(dim), currDim);  // store the current dim data in the compound tag of all vein dims
        }

        tags.setTag("vein_dims", dims);
    }
}
