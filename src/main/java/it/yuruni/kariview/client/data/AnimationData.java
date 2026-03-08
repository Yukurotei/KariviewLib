package it.yuruni.kariview.client.data;

import com.google.gson.annotations.SerializedName;
import it.yuruni.kariview.client.data.elements.GuiElementData;

import java.util.List;

public class AnimationData {
    private String id;
    private List<Keyframe> keyframes;
    private List<GuiElementData> elements;
    public transient String namespace = "";
    @SerializedName("end_action") private String endAction = "stop";
    @SerializedName("stop_animation") private StopAnimationData stopAnimation;
    @SerializedName("variable_watches") private List<VariableWatch> variableWatches;

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

    public String getNamespace() {
        return namespace;
    }

    public String getEndAction() {
        return endAction;
    }

    public StopAnimationData getStopAnimation() {
        return stopAnimation;
    }

    public List<VariableWatch> getVariableWatches() {
        return variableWatches;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setKeyframes(List<Keyframe> keyframes) {
        this.keyframes = keyframes;
    }

    public void setElements(List<GuiElementData> elements) {
        this.elements = elements;
    }

    public void setEndAction(String endAction) {
        this.endAction = endAction;
    }
}