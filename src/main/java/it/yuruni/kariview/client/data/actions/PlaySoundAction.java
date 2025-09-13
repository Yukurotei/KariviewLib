package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;

public class PlaySoundAction implements Action {
    @SerializedName("sound_id")
    private String soundId;
    private float volume;

    public String getSoundId() {
        return soundId;
    }

    public float getVolume() {
        return volume;
    }
}