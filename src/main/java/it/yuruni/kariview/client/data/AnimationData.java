package it.yuruni.kariview.client.data;

import it.yuruni.kariview.client.data.elements.GuiElementData;
import it.yuruni.kariview.client.data.elements.SoundElementData;

import java.util.List;

public class AnimationData {
    private String id;
    private List<Keyframe> keyframes;
    private List<GuiElementData> guiElements;
    private List<SoundElementData> soundElements;

    public String getId() {
        return id;
    }

    public List<Keyframe> getKeyframes() {
        return keyframes;
    }

    public List<GuiElementData> getGuiElements() {
        return guiElements;
    }

    public List<SoundElementData> getSoundElements() {
        return soundElements;
    }
}
