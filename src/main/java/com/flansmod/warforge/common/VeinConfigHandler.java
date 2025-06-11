package com.flansmod.warforge.common;

import lombok.AllArgsConstructor;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VeinConfigHandler {
    public final static String CONFIG_PATH = Paths.get("config/" + WarForgeMod.MODID + "/veins.cfg").toString();

    public static List<VeinEntry> loadVeins(String resourcePath) {
        List<VeinEntry> entries = new ArrayList<>();
        InputStream inputStream = VeinConfigHandler.class.getClassLoader()
                .getResourceAsStream(CONFIG_PATH);
        if (inputStream == null) {
            return null;
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