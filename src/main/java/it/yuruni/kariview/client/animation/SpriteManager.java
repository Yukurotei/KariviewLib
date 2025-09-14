package it.yuruni.kariview.client.animation;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpriteManager {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Map<String, List<ResourceLocation>> loadedSprites = new HashMap<>();

    public static List<ResourceLocation> loadSprites(String namespace, String pathPattern) {
        if (loadedSprites.containsKey(pathPattern)) {
            return loadedSprites.get(pathPattern);
        }

        List<ResourceLocation> spriteList = new ArrayList<>();
        File assetsDir = new File("kariviewlib/" + namespace + "/assets/textures/");
        if (!assetsDir.exists() || !assetsDir.isDirectory()) {
            LOGGER.error("Assets directory not found: " + assetsDir.getAbsolutePath());
            return spriteList;
        }

        String patternString = pathPattern.replace("*", "(\\d+)");
        Pattern pattern = Pattern.compile(patternString);

        File[] files = assetsDir.listFiles((dir, name) -> {
            Matcher matcher = pattern.matcher(name);
            return matcher.matches();
        });

        if (files == null || files.length == 0) {
            LOGGER.warn("No sprite files found for pattern: " + pathPattern);
            return spriteList;
        }

        // Sort the files by the number in their name to ensure correct order
        List<File> sortedFiles = new ArrayList<>(List.of(files));
        sortedFiles.sort((f1, f2) -> {
            Matcher m1 = pattern.matcher(f1.getName());
            Matcher m2 = pattern.matcher(f2.getName());
            if (m1.find() && m2.find()) {
                int num1 = Integer.parseInt(m1.group(1));
                int num2 = Integer.parseInt(m2.group(1));
                return Integer.compare(num1, num2);
            }
            return 0;
        });

        for (File file : sortedFiles) {
            try (FileInputStream stream = new FileInputStream(file)) {
                NativeImage image = NativeImage.read(stream);
                ResourceLocation location = new ResourceLocation(namespace, "textures/" + file.getName());
                Minecraft.getInstance().getTextureManager().register(location, new DynamicTexture(image));
                spriteList.add(location);
            } catch (IOException e) {
                LOGGER.error("Failed to load sprite file: " + file.getAbsolutePath(), e);
            }
        }

        loadedSprites.put(pathPattern, spriteList);
        return spriteList;
    }
}