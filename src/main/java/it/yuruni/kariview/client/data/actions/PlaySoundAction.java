package it.yuruni.kariview.client.data.actions;

import com.google.gson.annotations.SerializedName;
import com.mojang.logging.LogUtils;
import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.animation.AssetManager;
import it.yuruni.kariview.client.sound.RawAudio;
import org.slf4j.Logger;

import java.io.File;

public class PlaySoundAction implements Action {
    private static final Logger LOGGER = LogUtils.getLogger();

    @SerializedName("sound_id")
    private String soundId;
    private float volume;

    public String getSoundId() {
        return soundId;
    }

    public float getVolume() {
        return volume;
    }

    @Override
    public void execute(AnimationContext ctx) {
        File soundFile = AssetManager.loadSound(ctx.currentAnimation.getNamespace(), soundId);
        if (soundFile != null) {
            RawAudio.playOgg(soundFile.getAbsolutePath(), volume);
        } else {
            LOGGER.error("Failed to load sound file: {}:{}", ctx.currentAnimation.getNamespace(), soundId);
        }
    }
}
