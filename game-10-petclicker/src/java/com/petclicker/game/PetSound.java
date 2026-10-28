package com.petclicker.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Мягкие звуки кликера: тап, покупка, ивент. */
public final class PetSound {
    private static final int SR = 22050;

    private static void tone(double freq, int millis, double volume) {
        int n = SR * millis / 1000;
        short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            double env = 1.0 - (double) i / n;
            samples[i] = (short) (Short.MAX_VALUE * volume * env * Math.sin(2.0 * Math.PI * freq * i / SR));
        }
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SR)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(samples.length * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build();
        track.write(samples, 0, samples.length);
        track.play();
        new Thread(() -> {
            try {
                Thread.sleep(millis + 60L);
                track.release();
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    public void tap() {
        new Thread(() -> tone(620, 55, 0.16)).start();
    }

    public void buy() {
        new Thread(() -> {
            tone(520, 80, 0.18);
            try {
                Thread.sleep(70);
            } catch (InterruptedException ignored) {
            }
            tone(780, 110, 0.18);
        }).start();
    }

    public void event() {
        new Thread(() -> {
            double[] notes = {660, 880, 1100};
            for (double n : notes) {
                tone(n, 100, 0.2);
                try {
                    Thread.sleep(90);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }
}
