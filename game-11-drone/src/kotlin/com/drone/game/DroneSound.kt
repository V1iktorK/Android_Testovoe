package com.drone.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** Процедурный звук дрона: выстрел, попадание, взрыв. */
class DroneSound {
    private val sampleRate = 22050

    private fun tone(freq: Double, millis: Int, volume: Double = 0.2) {
        val n = sampleRate * millis / 1000
        val samples = ShortArray(n) { i ->
            val env = 1.0 - i.toDouble() / n
            val phase = (freq * i / sampleRate) % 1.0
            (Short.MAX_VALUE * volume * env * (2.0 * phase - 1.0)).toInt().toShort()
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

    fun explode() = Thread {
        tone(120.0, 320, 0.28)
        Thread.sleep(120)
        tone(90.0, 260, 0.22)
    }.start()
}
