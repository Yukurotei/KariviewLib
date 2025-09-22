package it.yuruni.kariview.client.effects;


import com.google.gson.annotations.SerializedName;

public class StepSpriteEffect extends AudioEffect {

    @SerializedName("step")
    private int spriteStep;
    @SerializedName("loop")
    private boolean loopSprite;
    @SerializedName("duration")
    private int delay;

    public StepSpriteEffect(String type) {
        super("STEP_SPRITE");
    }

    public int getSpriteStep() {
        return spriteStep;
    }

    public boolean isLoopSprite() {
        return loopSprite;
    }

    public int getDelay() {
        return delay;
    }
}
