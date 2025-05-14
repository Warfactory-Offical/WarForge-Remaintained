package com.flansmod.warforge.server;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.oredict.OreDictionary;

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
        if (registryName != null && stack.getItem().getRegistryName().equals(registryName)) {
            if (meta == -1) {
                return true;
            } else {
                return stack.getMetadata() == meta;
            }
        }
        return false;
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

    public class StackComparableResult {
        public final StackComparable compared;
        int has = 0;
        int required = 0;


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
