package it.yuruni.kariview.client.sound.state;

public class PulseState {
    public double targetScale;
    public double startScale;
    public double decay;
    public double defaultValue;
    public String easingType;
    public long lastPulseTime;
    public boolean isPulsing;

    public PulseState(double targetScale, double decay, double defaultValue, String easingType) {
        this.targetScale = targetScale;
        this.decay = decay;
        this.defaultValue = defaultValue;
        this.easingType = easingType;
        this.isPulsing = false;
        this.lastPulseTime = System.currentTimeMillis();
    }
}
