package it.yuruni.kariview.client.states;

public class ExtendState {
    public long lastBeatTime;
    public double targetValue;
    public double startValue;
    public String direction;
    public double decay;
    public double defaultValue;
    public double extendTime;
    public boolean isExtending;
    public String easing;

    public ExtendState(String direction, double targetValue, double decay, double defaultValue, double extendTime, String easing) {
        this.direction = direction;
        this.targetValue = targetValue;
        this.decay = decay;
        this.defaultValue = defaultValue;
        this.lastBeatTime = System.currentTimeMillis();
        this.isExtending = false;
        this.easing = easing;
        this.extendTime = extendTime;
    }
}