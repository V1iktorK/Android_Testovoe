package com.geomatch.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;

/** Процедурный звук match-3: свап, совпадение, спец-плитка, победа. */
public final class Sound {
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

    public static void swap() {
        new Thread(() -> tone(440, 50, 0.18)).start();
    }

    public static void match() {
        new Thread(() -> tone(660, 80, 0.2)).start();
    }

    public static void special() {
        new Thread(() -> {
            tone(520, 70, 0.22);
            try {
                Thread.sleep(60);
            } catch (InterruptedException ignored) {
            }
            tone(1040, 140, 0.22);
        }).start();
    }

    public static void win() {
        new Thread(() -> {
            double[] notes = {523.25, 659.25, 783.99, 1046.5};
            for (double n : notes) {
                tone(n, 130, 0.22);
                try {
                    Thread.sleep(110);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }
}
