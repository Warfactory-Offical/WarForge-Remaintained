package com.flansmod.warforge.common;

import lombok.AllArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class VeinConfigHandler {
    public final static Path CONFIG_PATH = Paths.get("config/" + WarForgeMod.MODID + "/veins.cfg");
    public static final List<String> EXAMPLE_YAML = Collections.unmodifiableList(Arrays.asList(
            "# Example vein definition format.",
            "# Each vein entry must contain:",
            "# - name: A unique translation key used for localization or identification.",
            "# - dims: A list of dimension weights, where:",
            "#     - id: The dimension ID (e.g. -1 for Nether, 0 for Overworld, 1 for End or custom).",
            "#     - weight: A double between 0.0 and 1.0 indicating the relative generation chance in that dimension.",
            "# - components: A list of ore components for the vein, where:",
            "#     - item: The item ID (e.g. minecraft:iron_ore) to generate in the vein.",
            "#     - yield: How many of this item the vein yields when selected.",
            "#     - weight: A double between 0.0 and 1.0 indicating how likely the item is to appear in the vein.",
            "#     - If component weights are omitted or empty, all are assumed to have a weight of 1.0.",
            "#",
            "# All fields are mandatory unless otherwise specified.",
            "# The number of dimension weights must match the number of dimension IDs.",
            "# The number of component weights must match the number of components.",
            "",
            "veins:",
            "  - name: warforge.veins.pure_iron",
            "    dims:",
            "      - id: -1",
            "        weight: 0.5",
            "      - id: 0",
            "        weight: 0.4215",
            "      - id: 1",
            "        weight: 1.0",
            "    components:",
            "      - item: minecraft:iron_ore",
            "        yield: 2",
            "        weight: 1.0",
            "      - item: minecraft:coal_ore",
            "        yield: 1",
            "        weight: 0.2"
    ));


    public static void writeStubIfEmpty() throws IOException {
        if (Files.notExists(CONFIG_PATH) || Files.size(CONFIG_PATH) == 0) {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.write(
                    CONFIG_PATH,
                    EXAMPLE_YAML,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE
            );
        }
    }

    public static List<VeinEntry> loadVeins() throws FileNotFoundException {
        List<VeinEntry> entries = new ArrayList<>();
        InputStream inputStream = new FileInputStream(CONFIG_PATH.toFile());
        if (inputStream == null) {
            throw new FileNotFoundException("File veins.cfg not found!");
        }

        Yaml yaml = new Yaml();
        List<Map<String, Object>> rawVeins;
        try {
            Map<String, Object> obj = yaml.load(inputStream);
            rawVeins = (List<Map<String, Object>>) obj.get("veins");
        } catch (ClassCastException e) {
            WarForgeMod.LOGGER.error("Failed to parse veins: ", e);
            return null;
        }
        for (Map<String, Object> rawVeinData : rawVeins) {
            try {
                String name = (String) rawVeinData.get("name");
                List<Map<String, Object>> dimsRaw = (List<Map<String, Object>>) rawVeinData.get("dims");
                List<DimWeight> dims = dimsRaw.stream().map(dim -> new DimWeight(
                                ((Number) dim.get("id")).intValue(),
                                ((Number) dim.get("weight")).doubleValue()))
                        .collect(Collectors.toList());
                List<Map<String, Object>> componentsRaw = (List<Map<String, Object>>) rawVeinData.get("components");
                List<Component> components = componentsRaw.stream()
                        .map(comp -> new Component(
                                (String) comp.get("item"),
                                ((Number) comp.get("yield")).intValue(),
                                ((Number) comp.get("weight")).doubleValue()))
                        .collect(Collectors.toList());
                entries.add(new VeinEntry(name, dims, components));
            } catch (ClassCastException e) {
                WarForgeMod.LOGGER.error("Failed to parse vein: ", e);
                continue;
            }

        }
        return entries;
    }

    @AllArgsConstructor
    public static class VeinEntry {
        final public String name;
        final public List<DimWeight> dims;
        final public List<Component> components;

        //Ugly workaround so I dont need to deal with networking
        public String serializeVeinEntry() {
            String dimIds = this.dims.stream()
                    .map(dw -> Integer.toString(dw.id))
                    .collect(Collectors.joining(", ", "{", "}"));

            String dimWeights = this.dims.stream()
                    .map(dw -> String.format("%.4f", dw.weight))
                    .collect(Collectors.joining(", ", "{", "}"));

            String components = this.components.stream()
                    .map(c -> c.yield + "~" + c.item)
                    .collect(Collectors.joining(", ", "{", "}"));

            String compWeights = this.components.stream()
                    .map(c -> String.format("%.4f", c.weight))
                    .collect(Collectors.joining(", ", "{", "}"));

            return String.join(", ", this.name, dimIds, dimWeights, components, compWeights);
        }
    }

    @AllArgsConstructor
    public static class DimWeight {
        final public int id;
        final public double weight;
    }

    @AllArgsConstructor
    public static class Component {
        final public String item;
        final public int yield;
        final public double weight;
    }

}