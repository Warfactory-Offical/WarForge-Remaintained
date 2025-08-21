package com.flansmod.warforge.common.blocks.models;

import com.flansmod.warforge.common.WarForgeMod;
import net.minecraft.util.ResourceLocation;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataRetrivalUtil {
    public static List<ResourceLocation> getResourcesFromPath(String dir) {
        var list = new ArrayList<ResourceLocation>();
        String modid = WarForgeMod.MODID;
        URI uri = null;
        try {
            uri = WarForgeMod.class.getProtectionDomain().getCodeSource().getLocation().toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        if (uri.getScheme().equals("jar")) {
            try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                String path = "assets/" + modid +   "/" + dir;
                var resources = fs.getPath(path);
                Files.walk(resources).forEach(p -> {
                    if (p.toString().endsWith(".png")) {
                        String rel = p.toString().replaceFirst("^/assets/"+modid+"/textures/", "").replaceFirst(".png$", "");
                        ResourceLocation rl = new ResourceLocation(modid, rel);
                        list.add(rl);
                    }
                });


            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return list;
    }
}
