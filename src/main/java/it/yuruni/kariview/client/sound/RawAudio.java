package it.yuruni.kariview.client.sound;

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

    private record DecodedAudio(ShortBuffer pcm, int sampleRate, int channels) {}
    private record CachedAudio(int alBufferId, ShortBuffer pcm, int sampleRate, int channels) {}
    private static final Map<String, DecodedAudio> decodedCache = new ConcurrentHashMap<>();
    private static final Map<String, CachedAudio> audioCache = new ConcurrentHashMap<>();

    public static void preloadOgg(String path) {
        if (audioCache.containsKey(path) || decodedCache.containsKey(path)) return;
        try (STBVorbisInfo info = STBVorbisInfo.malloc()) {
            IntBuffer error = BufferUtils.createIntBuffer(1);
            long decoder = STBVorbis.stb_vorbis_open_filename(path, error, null);
            if (decoder == 0) {
                LOGGER.error("Failed to preload OGG: " + path + ", Error Code: " + error.get(0));
                return;
            }
            STBVorbis.stb_vorbis_get_info(decoder, info);
            int channels = info.channels();
            int sampleRate = info.sample_rate();
            int lengthSamples = STBVorbis.stb_vorbis_stream_length_in_samples(decoder);
            ShortBuffer pcm = BufferUtils.createShortBuffer(lengthSamples * channels);
            STBVorbis.stb_vorbis_get_samples_short_interleaved(decoder, channels, pcm);
            pcm.rewind();
            STBVorbis.stb_vorbis_close(decoder);
            decodedCache.put(path, new DecodedAudio(pcm, sampleRate, channels));
        } catch (Exception e) {
            LOGGER.error("Failed to preload OGG: " + path, e);
        }
    }

    private static CachedAudio uploadToAL(String path) {
        CachedAudio existing = audioCache.get(path);
        if (existing != null) return existing;
        DecodedAudio decoded = decodedCache.get(path);
        if (decoded == null) {
            preloadOgg(path);
            decoded = decodedCache.get(path);
            if (decoded == null) return null;
        }
        int alBuffer = AL10.alGenBuffers();
        AL10.alBufferData(alBuffer, decoded.channels() == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16, decoded.pcm(), decoded.sampleRate());
        CachedAudio cached = new CachedAudio(alBuffer, decoded.pcm(), decoded.sampleRate(), decoded.channels());
        audioCache.put(path, cached);
        return cached;
    }

    public static void playOgg(String path, float volume) {
        CachedAudio cached = uploadToAL(path);
        if (cached == null) return;
        int source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, cached.alBufferId());
        AL10.alSourcef(source, AL10.AL_GAIN, volume);
        AL10.alSourcePlay(source);
        activeSources.add(source);
        sourceDataMap.put(source, new AudioData(cached.pcm(), cached.sampleRate(), cached.channels()));
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

    public static void clearCache() {
        for (CachedAudio cached : audioCache.values()) {
            AL10.alDeleteBuffers(cached.alBufferId());
        }
        audioCache.clear();
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
