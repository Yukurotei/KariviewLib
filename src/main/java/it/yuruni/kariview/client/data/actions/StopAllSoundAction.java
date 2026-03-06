package it.yuruni.kariview.client.data.actions;

import it.yuruni.kariview.client.animation.states.AnimationContext;
import it.yuruni.kariview.client.sound.RawAudio;

public class StopAllSoundAction implements Action {
    @Override
    public void execute(AnimationContext ctx) {
        RawAudio.stopAll();
    }
}
