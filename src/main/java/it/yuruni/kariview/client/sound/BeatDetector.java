package it.yuruni.kariview.client.sound;

import it.yuruni.kariview.client.animation.RawAudio;
import it.yuruni.kariview.client.data.AudioData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Mod.EventBusSubscriber
public class BeatDetector {
    private static final int BUFFER_SIZE = 1024;
    private static final int HISTORY_SIZE = 60;
    private final FFT fft;
    private final List<AudioElement> audioElements;
    private final Map<String, float[]> energyHistoryMap = new HashMap<>();
    private final Map<String, Integer> historyIndexMap = new HashMap<>();

    public BeatDetector() {
        this.fft = new FFT(BUFFER_SIZE);
        this.audioElements = new ArrayList<>();
        //Dummy
        this.audioElements.add(new AudioElement("example_image", "example.png", 1.25f, 120, 60, 2.0f, 2, "pulse", 1, 0.5f, 1.0f));
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            update();
            AudioStateManager.update();
        }
    }

    public void update() {
        for (int source : RawAudio.activeSources) {
            AudioData data = RawAudio.getAudioData(source);
            if (data == null) {
                RawAudio.cleanupSource(source);
                continue;
            }

            int playbackOffset = RawAudio.getPlaybackOffset(source);
            if (playbackOffset < BUFFER_SIZE) {
                continue;
            }

            float[] audioChunk = extractAudioChunk(data, playbackOffset);

            for (AudioElement element : audioElements) {
                //NOTE: Initialize history for element if it doesn't exist
                if (!energyHistoryMap.containsKey(element.elementId)) {
                    energyHistoryMap.put(element.elementId, new float[HISTORY_SIZE]);
                    historyIndexMap.put(element.elementId, 0);
                }

                boolean beatDetected = analyzeForElement(audioChunk, element);
                if (beatDetected) {
                    Minecraft.getInstance().player.sendSystemMessage(Component.literal("Beat detected for element: " + element.elementId + " at " + System.currentTimeMillis()));
                }
            }
        }
    }

    /**
     * Extracts a chunk of audio data from the ShortBuffer and converts it to a float array,
     * ensuring the array is always of BUFFER_SIZE.
     * @param audioData The AudioData object containing the ShortBuffer.
     * @param playbackOffset The current playback offset from the audio source.
     * @return A float array containing the audio samples, padded with zeros if necessary.
     */
    private float[] extractAudioChunk(AudioData audioData, int playbackOffset) {
        ShortBuffer pcm = audioData.getPcm();
        int channels = audioData.getChannels();
        int totalSamples = pcm.capacity() / channels;

        float[] audioChunk = new float[BUFFER_SIZE];

        // Calculate the starting index in the ShortBuffer
        int startSample = playbackOffset - BUFFER_SIZE;
        startSample = Math.max(0, startSample);

        // Copy the samples from the ShortBuffer
        for (int i = 0; i < BUFFER_SIZE; i++) {
            int currentSampleIndex = startSample + i;
            if (currentSampleIndex < totalSamples) {
                // Get the value from the buffer, accounting for channels
                // We'll use the left channel for simplicity with stereo audio
                int bufferIndex = currentSampleIndex * channels;
                short sample = pcm.get(bufferIndex);
                audioChunk[i] = ((float) sample) / 32768.0f;
            } else {
                // Pad with zeros if we've reached the end of the audio
                audioChunk[i] = 0.0f;
            }
        }

        return audioChunk;
    }

    private boolean analyzeForElement(float[] data, AudioElement element) {
        float[] fftData = new float[BUFFER_SIZE * 2];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            fftData[i * 2] = data[i];
            fftData[i * 2 + 1] = 0;
        }

        fft.transform(fftData);

        int sampleRate = 44100;
        float bandEnergy = 0;
        for (int i = 0; i < BUFFER_SIZE / 2; i++) {
            float frequency = (float) i * sampleRate / BUFFER_SIZE;
            if (frequency >= element.minHertz && frequency <= element.maxHertz) {
                float real = fftData[i * 2];
                float imag = fftData[i * 2 + 1];
                bandEnergy += real * real + imag * imag;
            }
        }

        float[] energyHistory = energyHistoryMap.get(element.elementId);
        int historyIndex = historyIndexMap.get(element.elementId);

        float averageEnergy = 0;
        for (float energy : energyHistory) {
            averageEnergy += energy;
        }
        averageEnergy /= HISTORY_SIZE;

        boolean beat = bandEnergy > averageEnergy * element.sensitivity;

        if (beat) {
            float currentVolume = (float) Math.sqrt(bandEnergy);
            float volumeRatio = currentVolume / element.maxVolume;
            float newValue = element.defaultValue + (element.maxValue - element.defaultValue) * volumeRatio;

            if (newValue > element.maxValue) {
                newValue = element.maxValue;
            }

            AudioStateManager.setElementValue(element.elementId, newValue);
        }

        energyHistory[historyIndex] = bandEnergy;
        historyIndexMap.put(element.elementId, (historyIndex + 1) % HISTORY_SIZE);

        return beat;
    }

    /*
    private boolean analyzeForBeats(float[] data) {
        System.out.printf("Frame[0]=%.4f Frame[1]=%.4f Frame[2]=%.4f%n",
                data[0], data[1], data[2]);
        float[] fftData = new float[BUFFER_SIZE * 2];
        for (int i = 0; i < BUFFER_SIZE; i++) {
            fftData[i * 2] = data[i];
            fftData[i * 2 + 1] = 0;
        }

        fft.transform(fftData);

        int sampleRate = 44100;
        float bandEnergy = 0;
        for (int i = 0; i < BUFFER_SIZE / 2; i++) {
            float frequency = (float) i * sampleRate / BUFFER_SIZE;
            if (frequency >= 60 && frequency <= 120) {
                float real = fftData[i * 2];
                float imag = fftData[i * 2 + 1];
                bandEnergy += real * real + imag * imag;
            }
        }

        float averageEnergy = 0;
        for (float energy : energyHistory) {
            averageEnergy += energy;
        }
        averageEnergy /= HISTORY_SIZE;

        float sensitivity = 1.2f;
        boolean beat = bandEnergy > averageEnergy * sensitivity;

        energyHistory[historyIndex] = bandEnergy;
        historyIndex = (historyIndex + 1) % HISTORY_SIZE;

        return beat;
    }
     */
}