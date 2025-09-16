package it.yuruni.kariview.client.sound;

public class AudioElement {
    public final String elementId;
    public final String texturePath;
    public final float sensitivity;
    public final int maxHertz;
    public final int minHertz;
    public final float maxValue;
    public final float maxVolume;
    public final String effect;
    public final int direction;
    public final float decay;
    public final float defaultValue;

    public AudioElement(String elementId, String texturePath, float sensitivity, int maxHertz, int minHertz, float maxValue, float maxVolume, String effect, int direction, float decay, float defaultValue) {
        this.elementId = elementId;
        this.texturePath = texturePath;
        this.sensitivity = sensitivity;
        this.maxHertz = maxHertz;
        this.minHertz = minHertz;
        this.maxValue = maxValue;
        this.effect = effect;
        this.direction = direction;
        this.decay = decay;
        this.defaultValue = defaultValue;
        this.maxVolume = maxVolume;
    }
}