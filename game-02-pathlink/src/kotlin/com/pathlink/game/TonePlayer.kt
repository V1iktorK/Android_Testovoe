package com.pathlink.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Процедурный звук: синус-тоны через AudioTrack (без аудиофайлов).
 */
class TonePlayer {
    private val sampleRate = 22050

    private fun tone(freq: Double, millis: Int, volume: Double = 0.25) {
        val n = sampleRate * millis / 1000
        val samples = ShortArray(n) { i ->
            val env = 1.0 - i.toDouble() / n
            (Short.MAX_VALUE * volume * env * sin(2.0 * PI * freq * i / sampleRate)).toInt().toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(sampleRate)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
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

    fun click() = Thread { tone(660.0, 60) }.start()

    fun win() = Thread {
        tone(523.25, 120)
        Thread.sleep(110)
        tone(659.25, 120)
        Thread.sleep(110)
        tone(783.99, 220)
    }.start()

    fun hint() = Thread {
        tone(880.0, 80)
        Thread.sleep(70)
        tone(1174.66, 120)
    }.start()
}
