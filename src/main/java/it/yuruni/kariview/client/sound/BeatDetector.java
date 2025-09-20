package it.yuruni.kariview.client.sound;

import it.yuruni.kariview.client.animation.AnimationManager;
import it.yuruni.kariview.client.data.AudioData;
import it.yuruni.kariview.client.data.actions.RegisterAudioElementAction;
import it.yuruni.kariview.client.effects.AudioEffect;
import it.yuruni.kariview.client.effects.ExtendEffect;
import it.yuruni.kariview.client.effects.PulseEffect;
import it.yuruni.kariview.client.effects.StepSpriteEffect;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.nio.ShortBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


@Mod.EventBusSubscriber
public class BeatDetector {
    private static final int BUFFER_SIZE = 1024;
    private static final int HISTORY_SIZE = 60;
    private final FFT fft;

    private static final ConcurrentMap<String, float[]> energyHistoryMap = new ConcurrentHashMap<>();
    private static final ConcurrentMap<String, Integer> historyIndexMap = new ConcurrentHashMap<>();

    public static final ConcurrentMap<String, RegisterAudioElementAction> registeredAudioElements = new ConcurrentHashMap<>();

    public BeatDetector() {
        this.fft = new FFT(BUFFER_SIZE);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            update();
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

            for (Map.Entry<String, RegisterAudioElementAction> entry : registeredAudioElements.entrySet()) {
                String elementId = entry.getKey();
                RegisterAudioElementAction elementConfig = entry.getValue();

                if (!energyHistoryMap.containsKey(elementId)) {
                    energyHistoryMap.put(elementId, new float[HISTORY_SIZE]);
                    historyIndexMap.put(elementId, 0);
                }

                analyzeAndDispatch(audioChunk, elementConfig, elementId);
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

    private void analyzeAndDispatch(float[] data, RegisterAudioElementAction elementConfig, String elementId) {
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
            if (frequency >= elementConfig.getMinHertz() && frequency <= elementConfig.getMaxHertz()) {
                float real = fftData[i * 2];
                float imag = fftData[i * 2 + 1];
                bandEnergy += real * real + imag * imag;
            }
        }

        float[] energyHistory = energyHistoryMap.get(elementId);
        int historyIndex = historyIndexMap.get(elementId);

        float averageEnergy = 0;
        for (float energy : energyHistory) {
            averageEnergy += energy;
        }
        averageEnergy /= HISTORY_SIZE;

        boolean beat = bandEnergy > averageEnergy * elementConfig.getSensitivity();

        if (beat) {
            for (AudioEffect effect : elementConfig.getEffects()) {
                if (effect.getType().equalsIgnoreCase("PULSE")) {
                    PulseEffect pulseEffect = (PulseEffect) effect;

                    float currentVolume = (float) Math.sqrt(bandEnergy);
                    float volumeRatio = currentVolume / elementConfig.getMaxVolume();
                    float pulseValue = pulseEffect.getDefaultValue() + (pulseEffect.getMaxValue() - pulseEffect.getDefaultValue()) * volumeRatio;
                    if (pulseValue > pulseEffect.getMaxValue()) {
                        pulseValue = pulseEffect.getMaxValue();
                    }

                    AnimationManager.triggerPulse(
                            elementId,
                            pulseValue,
                            pulseEffect.getDecay(),
                            pulseEffect.getDefaultValue(),
                            elementConfig.getEasingType()
                    );
                } else if (effect.getType().equalsIgnoreCase("STEP_SPRITE")) {
                    //Check if element is sprite
                    if (AnimationManager.spriteStates.containsKey(elementId)) {
                        StepSpriteEffect stepEffect = (StepSpriteEffect) effect;
                        AnimationManager.triggerSpriteChange(elementId, stepEffect.getSpriteStep(), stepEffect.getDelay(), stepEffect.isLoopSprite());
                    }
                } else if (effect.getType().equalsIgnoreCase("EXTEND")) {
                    ExtendEffect extendEffect = (ExtendEffect) effect;

                    float currentVolume = (float) Math.sqrt(bandEnergy);
                    float volumeRatio = currentVolume / elementConfig.getMaxVolume();
                    double extendValue = extendEffect.getDefaultValue() + (extendEffect.getTargetValue() - extendEffect.getDefaultValue()) * volumeRatio;
                    if (extendValue > extendEffect.getTargetValue()) {
                        extendValue = extendEffect.getTargetValue();
                    }

                    AnimationManager.triggerExtend(
                            elementId,
                            extendEffect.getDirection(),
                            extendValue,
                            extendEffect.getDuration(),
                            extendEffect.getDecay(),
                            extendEffect.getDefaultValue(),
                            extendEffect.getExtendTime(),
                            elementConfig.getEasingType()
                    );
                }
            }
        }

        energyHistory[historyIndex] = bandEnergy;
        historyIndexMap.put(elementId, (historyIndex + 1) % HISTORY_SIZE);
    }
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
            if (frequency >= 20 && frequency <= 100) {
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