package it.yuruni.kariview.client.data.actions;

public class PlaySoundAction implements Action {
    private String elementId;
    private float volume;

    public String getElementId() {
        return elementId;
    }

    public float getVolume() {
        return volume;
    }
}
