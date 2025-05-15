package com.flansmod.warforge.server;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpgradeHandler {


    public static final List<String> STUB = Arrays.asList(
            "# Upgrade Levels Configuration",
            "# Each level starts with 'level:<number>[<claim limit>]'",
            "# Claim Limit must be a positive number above 0, -1 in claim limits denotes infinite claims",
            "# Followed by one or more '(ore | item):<entry>[count]' lines",
            "# Lack of count will assume 1",
            "# Entries can be:",
            "#   - OreDict name (e.g. ore:oreIron[64])",
            "#   - Registry name (e.g. item:minecraft:diamond[1])",
            "#   - Registry with metadata (e.g. item:modid:some_item:2)",
            "############################################################################################",
            "# Example:",
            "level:0[5]",
            "level:1[10]",
            "ore:ingotIron[64]",
            "item:minecraft:diamond",
            "",
            "level:2[15]",
            "item:modid:custom_item:3"
    );
    protected HashMap<StackComparable, Integer>[] LEVELS;

    public int[] getLIMITS() {
        return LIMITS;
    }

    public HashMap<StackComparable, Integer>[] getLEVELS() {
        return LEVELS;
    }

    protected int[] LIMITS;

    public void setLevelAndLimits(int level, HashMap<StackComparable, Integer> requrements, int limit) {
        //Only ran once on clientside per join, so whatever
        if (level >= LEVELS.length) {
            int newSize = Math.max(level + 1, LEVELS.length * 2);

            // Extend LEVELS
            HashMap<StackComparable, Integer>[] newLevels = Arrays.copyOf(LEVELS, newSize);
            LEVELS = newLevels;

            // Extend LIMITS
            int[] newLimits = Arrays.copyOf(LIMITS, newSize);
            LIMITS = newLimits;
        }
        LEVELS[level] = requrements;
        LIMITS[level] = limit;
    }



    public static void writeStubIfEmpty(Path filePath) throws IOException {
        if (Files.notExists(filePath) || Files.size(filePath) == 0) {
            Files.createDirectories(filePath.getParent());
            Files.write(
                    filePath,
                    STUB,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        }
    }

    public static void parseConfig(Path path) throws IOException {
        List<Map<StackComparable, Integer>> levels = new ArrayList<>();
        List<Integer> claims = new ArrayList<>();

        // Always initialize level 0 as free, empty
        levels.add(new HashMap<>());
        claims.add(-1); // Default claim limit for level 0

        Map<StackComparable, Integer> current = null;
        int currentLevel = 0;

        for (String line : Files.readAllLines(path)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            if (line.startsWith("level:")) {
                String levelSpec = line.substring(6).trim();
                Matcher matcher = Pattern.compile("(\\d+)\\[(\\d+|-1)]").matcher(levelSpec);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid level format: " + line);
                }

                currentLevel = Integer.parseInt(matcher.group(1));
                int claimsAtLevel = Integer.parseInt(matcher.group(2));

                if (claimsAtLevel != -1 && claimsAtLevel <= 0)
                    throw new IllegalArgumentException("Claims must be > 0 or -1");

                while (levels.size() <= currentLevel) {
                    levels.add(new HashMap<>());
                    claims.add(-1); // Default to -1 if not defined yet
                }

                claims.set(currentLevel, claimsAtLevel);
                current = levels.get(currentLevel);
            } else {
                if (current == null) {
                    throw new IllegalStateException("Item defined before any level");
                }

                if (currentLevel == 0) {
                    WarForgeMod.LOGGER.warn("Ignoring item/ore line at level 0: " + line);
                    continue;
                }

                String type = null;
                if (line.startsWith("item:")) {
                    type = "item";
                } else if (line.startsWith("ore:")) {
                    type = "ore";
                } else {
                    throw new IllegalArgumentException("Unknown entry type: " + line);
                }

                String content = line.substring(type.length() + 1).trim();
                Matcher matcher = Pattern.compile("(.+?)(\\[(\\d+)])?$").matcher(content);
                if (!matcher.matches()) {
                    throw new IllegalArgumentException("Invalid format for line: " + line);
                }

                String rawEntry = matcher.group(1);
                int count = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 1;

                StackComparable sc;

                if ("ore".equals(type)) {
                    sc = new StackComparable().toOredict(rawEntry);
                } else {
                    String[] parts = rawEntry.split(":");
                    if (parts.length == 2) {
                        sc = new StackComparable(parts[0] + ":" + parts[1]);
                    } else if (parts.length == 3) {
                        sc = new StackComparable(parts[0] + ":" + parts[1], Integer.parseInt(parts[2]));
                    } else {
                        throw new IllegalArgumentException("Invalid item format: " + rawEntry);
                    }

                    ResourceLocation id = new ResourceLocation(sc.registryName);
                    if (!ForgeRegistries.ITEMS.containsKey(id)) {
                        WarForgeMod.LOGGER.warn("UpgradeHandler config: Item " + id + " does not exist. Level " + currentLevel + " may become inaccessible.");
                    }
                }

                current.put(sc, count);
            }
        }

        // Validate monotonic increasing claim limits
        for (int i = 1; i < claims.size(); i++) {
            if (claims.get(i) != -1 && claims.get(i - 1) != -1 && claims.get(i) < claims.get(i - 1)) {
                throw new IllegalStateException("Claim limit at level " + i + " is less than previous level");
            }
        }

        int size = levels.size();
        WarForgeMod.UPGRADE_HANDLER.LEVELS = new HashMap[size];
        WarForgeMod.UPGRADE_HANDLER.LIMITS = new int[size];
        for (int i = 0; i < size; i++) {
            WarForgeMod.UPGRADE_HANDLER.LEVELS[i] = new HashMap<>(levels.get(i));
            WarForgeMod.UPGRADE_HANDLER.LIMITS[i] = claims.get(i);
        }
    }



    public HashMap<StackComparable, Integer> getRequirementsFor(int level) {
        if(level >= LEVELS.length)
            return null;
        return LEVELS[level];
    }

    public int getClaimLimitForLevel(int level) {
        if (level > LIMITS.length || level < 0)
            return -1;
        else
            return LIMITS[level];
    }

}
