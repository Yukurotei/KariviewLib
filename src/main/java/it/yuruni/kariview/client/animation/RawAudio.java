package it.yuruni.kariview.client.animation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.lwjgl.stb.STBVorbis;
import org.lwjgl.stb.STBVorbisInfo;

import java.nio.IntBuffer;
import java.nio.ShortBuffer;

public class RawAudio {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void playOgg(String path) {
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

            int buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, channels == 1 ? AL10.AL_FORMAT_MONO16 : AL10.AL_FORMAT_STEREO16,
                    pcm, sampleRate);

            int source = AL10.alGenSources();
            AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
            AL10.alSourcePlay(source);

            STBVorbis.stb_vorbis_close(decoder);
        }
    }
}

