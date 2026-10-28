package com.neonidle.game;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import java.util.concurrent.LinkedBlockingQueue;

/** Процедурный звук кликера: синус/пила через AudioTrack, без файлов. */
public final class Synth {
    private static final int SR = 22050;

    private static void tone(double freq, int millis, double volume, boolean saw) {
        int n = SR * millis / 1000;
        short[] samples = new short[n];
        for (int i = 0; i < n; i++) {
            double env = 1.0 - (double) i / n;
            double phase = (freq * i / SR) % 1.0;
            double wave = saw ? (2.0 * phase - 1.0) : Math.sin(2.0 * Math.PI * freq * i / SR);
            samples[i] = (short) (Short.MAX_VALUE * volume * env * wave);
        }
        AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setSampleRate(SR)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
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

    public static void click() {
        new Thread(() -> tone(220, 50, 0.22, true)).start();
    }

    public static void buy() {
        new Thread(() -> {
            tone(330, 70, 0.2, true);
            try {
                Thread.sleep(60);
            } catch (InterruptedException ignored) {
            }
            tone(495, 100, 0.2, true);
        }).start();
    }

    public static void prestige() {
        new Thread(() -> {
            double[] notes = {261.63, 329.63, 392.0, 523.25};
            for (double n : notes) {
                tone(n, 140, 0.22, false);
                try {
                    Thread.sleep(120);
                } catch (InterruptedException ignored) {
                }
            }
        }).start();
    }
}
