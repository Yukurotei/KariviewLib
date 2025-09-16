package it.yuruni.kariview.client.sound;

import java.util.HashMap;
import java.util.Map;

public class AudioStateManager {
    private static final Map<String, Float> elementState = new HashMap<>();
    private static final Map<String, AudioElement> elements = new HashMap<>();

    public static void registerElement(AudioElement element) {
        elements.put(element.elementId, element);
    }

    public static void setElementValue(String elementId, float value) {
        if (elements.containsKey(elementId)) {
            elementState.put(elementId, value);
        }
    }

    public static float getElementValue(String elementId, float defaultValue) {
        return elementState.getOrDefault(elementId, defaultValue);
    }

    public static void update() {
        for (Map.Entry<String, Float> entry : elementState.entrySet()) {
            String elementId = entry.getKey();
            float currentValue = entry.getValue();
            AudioElement element = elements.get(elementId);

            if (element != null && currentValue > element.defaultValue) {
                float newValue = currentValue - element.decay;
                if (newValue < element.defaultValue) {
                    newValue = element.defaultValue;
                }
                elementState.put(elementId, newValue);
            }
        }
    }
}