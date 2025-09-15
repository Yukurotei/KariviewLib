package it.yuruni.kariview.client.data;

import java.nio.ShortBuffer;

public class AudioData {
    private final ShortBuffer pcm;
    private final int sampleRate;
    private final int channels;

    public AudioData(ShortBuffer pcm, int sampleRate, int channels) {
        this.pcm = pcm;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public ShortBuffer getPcm() {
        return pcm;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getChannels() {
        return channels;
    }
}