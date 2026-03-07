package it.yuruni.kariview.client.data;

import com.google.gson.Gson;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VariableManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();
    private static final Map<String, Map<String, String>> variables = new ConcurrentHashMap<>();

    public static void loadVariables(String namespace) {
        File f = new File("kariviewlib/" + namespace + "/variables.json");
        if (!f.exists()) {
            variables.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
            return;
        }
        try {
            String content = Files.readString(f.toPath());
            Map<String, String> parsed = GSON.fromJson(content, new com.google.gson.reflect.TypeToken<Map<String, String>>(){}.getType());
            variables.put(namespace, new ConcurrentHashMap<>(parsed));
        } catch (IOException e) {
            LOGGER.error("Failed to load variables for namespace: {}", namespace, e);
            variables.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>());
        }
    }

    public static String get(String namespace, String name) {
        Map<String, String> ns = variables.get(namespace);
        return ns != null ? ns.get(name) : null;
    }

    public static void set(String namespace, String name, String value) {
        variables.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>()).put(name, value);
    }
}
