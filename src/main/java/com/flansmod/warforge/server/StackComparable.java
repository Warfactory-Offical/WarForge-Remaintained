package com.flansmod.warforge.server;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.util.Objects;

public class StackComparable {
    protected String oredict = null;
    protected String registryName = null;
    protected short meta = -1;

    public String getOredict() {
        return oredict;
    }

    public String getRegistryName() {
        return registryName;
    }

    public short getMeta() {
        return meta;
    }

    public StackComparable() {
    }

    public StackComparable(String registryName) {
        this.registryName = registryName;
        meta = -1;
    }

    public StackComparable(String registryName, int meta) {
        this.registryName = registryName;
        this.meta = (short) meta;
    }

    // will just pick the first registry name from the given oredict
    public static StackComparable parseArbitraryResource(String resource) {
        if (OreDictionary.doesOreNameExist(resource)) {
            return new StackComparable().toOredict(resource);
        }

        // if we have SOURCE:item_name:meta, then treat as stack with meta
        if (resource.indexOf(':') != resource.lastIndexOf(':')) {
            int lastSepIndex = resource.lastIndexOf(':');
            return new StackComparable(resource.substring(0, lastSepIndex), Integer.parseInt(resource.substring(lastSepIndex)));
        }

        return new StackComparable(resource);
    }

    public static String[] getOreDictNames(ItemStack stack) {
        int[] ids = OreDictionary.getOreIDs(stack);
        String[] names = new String[ids.length];

        for (int i = 0; i < ids.length; i++) {
            names[i] = OreDictionary.getOreName(ids[i]);
        }

        return names;
    }

    public static StackComparable readFromNBT(NBTTagCompound tag) {
        StackComparable sc = new StackComparable();
        if (tag.hasKey("ore"))
            sc.oredict = tag.getString("ore");
        else if (tag.hasKey("name")) {
            sc.registryName = tag.getString("name");
            if (tag.hasKey("meta")) {
                sc.meta = tag.getShort("meta");
                return sc;
            }
        }
        sc.meta = -1;
        return sc;
    }

    public StackComparable toOredict(String oredict) {
        this.oredict = oredict;
        registryName = null;
        this.meta = -1;
        return this;
    }

    // return an item stack of 1 corresponding to this item, preferring oredict use and returning its first result
    public ItemStack toItem() {
        return toItem(1);
    }

    // return the corresponding item stack w/ the amount provided, preferring oredict and if possible the first oredict result
    public ItemStack toItem(int amount) {
        return toItem(amount, 0);
    }

    // return the item stack corresponding to this stack comparable, based on the amount and oredict index provided
    // oredict will be used first, assuming both oredict and the registry name are present
    public ItemStack toItem(int amount, int oredictIndex) {
        // try to get the oredict of the ore
        if (this.oredict != null && OreDictionary.doesOreNameExist(oredict)) {
            var oredictOptions = OreDictionary.getOres(oredict, false);
            return oredictOptions.get(oredictIndex).copy();  // if we don't copy, trying to use the itemstack can modify/ consume it
        }

        // no matching oredict and no registry name means there is no item
        if (registryName == null) { return null; }

        // check that the resource exists
        ResourceLocation resource = new ResourceLocation(registryName);
        if (!ForgeRegistries.ITEMS.containsKey(resource)) { return null; }  // cannot return item that doesnt exist

        // likely redundant check to ensure item is not null, but intellij complains and I can't guarantee that the
        // registry wouldn't return a null value if the key exists
        Item item = ForgeRegistries.ITEMS.getValue(resource);
        if (item == null) { return null; }

        // create the itemstack with the metadata needed, if applicable
        if (meta == -1) { return new ItemStack(item, amount); }
        else { return new ItemStack(item, amount, meta); }
    }

    public boolean equals(ItemStack stack) {
        //Oredict check
        if (oredict != null && !oredict.isEmpty()) {
            String[] stackOreDict = getOreDictNames(stack);
            for (String stackOreDictEntry : stackOreDict) {
                if (oredict.equals(stackOreDictEntry))
                    return true;
            }

        }

        //Registry key check
        if (registryName != null && stack.getItem().getRegistryName().toString().equals(registryName)) {
            if (meta == -1) {
                return true;
            } else {
                return stack.getMetadata() == meta;
            }
        }

        return false;
    }

    // attempts all possible checks to guarantee inequality to passed stack comparable
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StackComparable otherStack)) { return false; }

        // try to compare registry name and meta, then oredict, then try to compare the item result
        return (Objects.equals(registryName, otherStack.registryName) && meta == otherStack.meta) ||
                Objects.equals(oredict, otherStack.oredict) ||
                this.equals(otherStack.toItem());  // convert to item and check (costly and unlikely to work)
    }

    @Override
    public String toString() {
        return "RegName: " + (registryName == null ? "NULL" : registryName) + ", Oredict: " +
                (oredict == null ? "NULL" : oredict) + ", Meta: " + meta;
    }

    // adds the meta to the hashes of oredict and the registry name, using 0 as the hash for null strings
    @Override
    public int hashCode() {
        int hash = meta;
        if (oredict != null) { hash += oredict.hashCode(); }
        if (registryName != null) { hash += registryName.hashCode(); }
        return hash;
    }

    public StackComparableResult toStackComparableResult(int playerInvCount, int required) {
        return new StackComparableResult(this, playerInvCount, required);
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        if (this.oredict != null) {
            tag.setString("ore", oredict);
        } else if (this.registryName != null) {
            tag.setString("name", this.registryName);
            if (this.meta != -1) {
                tag.setShort("meta", this.meta);
            }
        }
        return tag;
    }

    public static class StackComparableResult {
        public final StackComparable compared;
        public int has = 0;
        public int required = 0;


        public StackComparableResult(StackComparable compared, int has, int required) {
            this.compared = compared;
            this.has = has;
            this.required = required;
        }

        public static StackComparableResult readFromNBT(NBTTagCompound tag) {
            StackComparable sc = StackComparable.readFromNBT(tag);
            return sc.toStackComparableResult(tag.getInteger("has"), tag.getInteger("need"));
        }

        public boolean isEnough() {
            return has > required;
        }

        public NBTTagCompound encodeToNBT() {
            NBTTagCompound tag = compared.writeToNBT();
            tag.setInteger("has", has);
            tag.setInteger("need", required);
            return tag;
        }
    }

}
