package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.oredict.OreDictionary;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpgradeHandler {


    public StackComparable[][] LEVELS;
    public int[] LIMITS;
    public static final List<String> DEFAULT_CONFIG = Arrays.asList(
            "# Upgrade Levels Configuration",
            "# Each level starts with 'level:<number>[<claim limit>]'",
            "# Claim Limit must be a positive number above 0, -1 in claim limits denotes infinite claims",
            "# Followed by one or more 'item:<entry>' lines",
            "# Entries can be:",
            "#   - OreDict name (e.g. item:oreIron)",
            "#   - Registry name (e.g. item:minecraft:diamond)",
            "#   - Registry with metadata (e.g. item:modid:some_item:2)",
            "############################################################################################",
            "# Example:",
            "level:0[5]",
            "item:oreIron",
            "item:minecraft:diamond",
            "",
            "level:1[10]",
            "item:modid:custom_item:3"
    );

    public static void parseConfig(Path path) throws IOException {
        List<List<StackComparable>> levels = new ArrayList<>();
        List<StackComparable> current = null;
        List<Integer> claims = new ArrayList<>();

        // Initialize claims with a default value (-1 for infinite)
        claims.add(-1); // Level 0 default claim

        for (String line : Files.readAllLines(path)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Parse level and claims
            if (line.startsWith("level:")) {
                String levelSpec = line.substring(6).trim();
                Matcher matcher = Pattern.compile("(\\d+)\\[(\\d+|-1)]").matcher(levelSpec);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid level format: " + line);
                }
                int level = Integer.parseInt(matcher.group(1));
                int claimsAtLevel = Integer.parseInt(matcher.group(2));

                if (claimsAtLevel != -1 && claimsAtLevel <= 0)
                    throw new IllegalArgumentException("Claims must be > 0 or -1");

                while (levels.size() <= level) {
                    levels.add(new ArrayList<>());
                    claims.add(-1); // Default claim value
                }

                current = levels.get(level);
                claims.set(level, claimsAtLevel);
            } else if (line.startsWith("item:")) {
                if (current == null) throw new IllegalStateException("Item defined before any level");

                String spec = line.substring(5).trim();
                String[] parts = spec.split(":");

                StackComparable sc;
                if (parts.length == 1) {
                    // OreDict entry
                    sc = new StackComparable(parts[0]);
                } else if (parts.length == 2) {
                    // registryName only
                    sc = new StackComparable(parts[0] + ":" + parts[1]);
                } else if (parts.length == 3) {
                    // registryName + meta
                    sc = new StackComparable(parts[0] + ":" + parts[1], Integer.parseInt(parts[2]));
                } else {
                    throw new IllegalArgumentException("Invalid item format: " + spec);
                }

                // Check if the item exists in the registry
                if (sc.registryName != null) {
                    ResourceLocation id = new ResourceLocation(sc.registryName);
                    if (!ForgeRegistries.ITEMS.containsKey(id)) {
                        WarForgeMod.LOGGER.warn("UpgradeHandler config: Item " + id + " does not exist. Level " + levels.indexOf(current) + " may become inaccessible.");
                    }
                }

                current.add(sc);
            }
        }

        // Ensure claim limits are non-decreasing, preventing deadlocks
        for (int i = 1; i < claims.size(); i++) {
            if (claims.get(i) != -1 && claims.get(i - 1) != -1 && claims.get(i) < claims.get(i - 1)) {
                throw new IllegalStateException("Claim limit at level " + i + " is less than previous level");
            }
        }

        // Convert List<List<>> to array[][]
        WarForgeMod.UPGRADE_HANDLER.LEVELS = new StackComparable[levels.size()][];
        WarForgeMod.UPGRADE_HANDLER.LIMITS = new int[levels.size()];
        for (int i = 0; i < levels.size(); i++) {
            WarForgeMod.UPGRADE_HANDLER.LEVELS[i] = levels.get(i).toArray(new StackComparable[0]);
            WarForgeMod.UPGRADE_HANDLER.LIMITS[i] = claims.get(i);
        }
    }


    public StackComparable[] getReuquiremetsFor(int level) {
        return LEVELS[level];
    }

    public static class StackComparable {
        Set<String> oredict;
        String registryName = null;
        short meta;

        public StackComparable(String... oredict) {
            this.oredict = new HashSet<>(Arrays.asList(oredict));
            meta = -1;
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

        public boolean equals(ItemStack stack) {
            //Oredict check
            if (oredict != null && !oredict.isEmpty()) {
                String[] stackOreDict = getOreDictNames(stack);
                for (String stackOreDictEntry : stackOreDict) {
                    if (oredict.contains(stackOreDictEntry))
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

    }
}
