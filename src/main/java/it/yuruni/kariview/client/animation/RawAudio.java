package it.yuruni.kariview.client.animation;

import it.yuruni.kariview.client.data.AudioData;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.AL11;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE)
public class RawAudio {
    private static final Logger LOGGER = LogManager.getLogger();
    public static final List<Integer> activeSources = new ArrayList<>();
    private static final Map<Integer, AudioData> sourceDataMap = new ConcurrentHashMap<>();

    public static void playOgg(String path, float volume) {
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            IntBuffer error = BufferUtils.createIntBuffer(1);
            long decoder = STBVorbis.stb_vorbis_open_filename(path, error, null);
            if (decoder == 0) {
                LOGGER.error("Failed to open OGG: " + path + ", Error Code: " + error.get(0));
                return;
            }

            STBVorbis.stb_vorbis_get_info(decoder, info);
            int channels = info.channels();
            int sampleRate = info.sample_rate();

            int lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            ShortBuffer pcm = BufferUtils.createShortBuffer(lengthSamples * channels);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            pcm.rewind();

            int buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                    pcm, sampleRate);

            int source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcef(source, AL10.AL_GAIN, volume);
            AL10.alSourcePlay(source);

            activeSources.add(source);
            sourceDataMap.put(source, new AudioData(pcm, sampleRate, channels));

            // We do not close the decoder here as the stream may still be playing.
            // A separate cleanup mechanism is needed.
        }
    }

    public static void stopAll() {
        for (int source : activeSources) {
            if (AL10.alIsSource(source)) {
                AL10.alSourceStop(source);
                AL10.alDeleteSources(source);
            }
        }
        activeSources.clear();
        sourceDataMap.clear();
    }

    public static AudioData getAudioData(int source) {
        return sourceDataMap.get(source);
    }

    public static int getPlaybackOffset(int source) {
        return AL10.alGetSourcei(source, AL11.AL_SAMPLE_OFFSET);
    }

    public static void cleanupSource(int source) {
        activeSources.remove((Integer) source);
        sourceDataMap.remove(source);
    }
}
