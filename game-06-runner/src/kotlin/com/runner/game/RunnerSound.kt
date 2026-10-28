package com.runner.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** Процедурный звук раннера: прыжок, монета, удар, рекорд. */
class RunnerSound {
    private val sampleRate = 22050

    private fun tone(freq: Double, millis: Int, volume: Double = 0.22) {
        val n = sampleRate * millis / 1000
        val samples = ShortArray(n) { i ->
            val env = 1.0 - i.toDouble() / n
            (Short.MAX_VALUE * volume * env * sin(2.0 * PI * freq * i / sampleRate)).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Thread {
            Thread.sleep(millis + 60L)
            try {
                track.release()
            } catch (_: Exception) {
            }
        }.start()
    }

    fun jump() = Thread { tone(520.0, 90) }.start()
    fun coin() = Thread {
        tone(880.0, 60)
        Thread.sleep(50)
        tone(1320.0, 80)
    }.start()
    fun hit() = Thread { tone(160.0, 220, 0.3) }.start()
    fun record() = Thread {
        tone(660.0, 120)
        Thread.sleep(100)
        tone(880.0, 120)
        Thread.sleep(100)
        tone(1100.0, 220)
    }.start()
}
