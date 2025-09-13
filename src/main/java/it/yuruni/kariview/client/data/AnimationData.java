package it.yuruni.kariview.client.data;

import it.yuruni.kariview.client.data.elements.GuiElementData;

import java.util.List;

public class AnimationData {
    private String id;
    private List<Keyframe> keyframes;
    private List<GuiElementData> elements;

    public String getId() {
        return id;
    }

    public List<Keyframe> getKeyframes() {
        return keyframes;
    }

    public List<GuiElementData> getElements() {
        return elements;
    }

    public long getTotalDuration() {
        long totalDuration = 0;
        if (keyframes != null && !keyframes.isEmpty()) {
            for (Keyframe keyframe : keyframes) {
                if (keyframe.getTimestamp() > totalDuration) {
                    totalDuration = keyframe.getTimestamp();
                }
            }
        }
        return totalDuration;
    }

    public GuiElementData getElementById(String id) {
        if (elements != null) {
            for (GuiElementData element : elements) {
                if (element != null && element.getId().equals(id)) {
                    return element;
                }
            }
        }
        return null;
    }
}