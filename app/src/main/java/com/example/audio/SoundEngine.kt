package com.example.audio

import android.media.AudioAttributes
import android.media.SoundPool
import com.example.chess.MoveSoundKind
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SoundEngine(cacheDir: File? = null) {

  private enum class Wave { SINE, TRIANGLE, SQUARE, SAWTOOTH }

  private enum class SoundCue {
    CLICK,
    ERROR,
    SUCCESS,
    MOVE,
    CAPTURE,
    CHECK,
    CHECKMATE,
    CASTLE,
    PROMOTE,
    PREVIEW,
  }

  private data class ToneSpec(
    val freqHz: Double,
    val durationSec: Double,
    val wave: Wave = Wave.SINE,
    val gain: Double = 0.18,
    val offsetSec: Double = 0.0,
  )

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val soundIds = ConcurrentHashMap<SoundCue, Int>()
  private val loadedIds = ConcurrentHashMap.newKeySet<Int>()
  private var soundPool: SoundPool? = null

  @Volatile
  var muted: Boolean = false

  @Volatile
  var volume: Float = 0.6f

  init {
    if (cacheDir != null) {
      initSoundPool(cacheDir)
    }
  }

  private fun initSoundPool(cacheDir: File) {
    try {
      val pool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
          AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        )
        .build()
      pool.setOnLoadCompleteListener { _, sampleId, status ->
        if (status == 0) loadedIds.add(sampleId)
      }
      soundPool = pool

      scope.launch {
        val dir = File(cacheDir, "rookie_sfx_v1").apply { mkdirs() }
        for (cue in SoundCue.entries) {
          val file = File(dir, "${cue.name.lowercase()}.wav")
          if (!file.exists() || file.length() <= 44L) {
            writeCueWav(cue, file)
          }
          val id = pool.load(file.absolutePath, 1)
          soundIds[cue] = id
        }
      }
    } catch (_: Throwable) {
      // Safe fallback in unit test environments
    }
  }

  fun playClick() = playCue(SoundCue.CLICK)

  fun playError() = playCue(SoundCue.ERROR)

  fun playSuccess() = playCue(SoundCue.SUCCESS)

  fun playMoveSound(kind: MoveSoundKind, delayMs: Long = 0L) {
    val cue = when (kind) {
      MoveSoundKind.MOVE -> SoundCue.MOVE
      MoveSoundKind.CAPTURE -> SoundCue.CAPTURE
      MoveSoundKind.CHECK -> SoundCue.CHECK
      MoveSoundKind.CHECKMATE -> SoundCue.CHECKMATE
      MoveSoundKind.CASTLE -> SoundCue.CASTLE
      MoveSoundKind.PROMOTE -> SoundCue.PROMOTE
    }
    if (delayMs > 0L) {
      scope.launch {
        delay(delayMs)
        playCue(cue)
      }
    } else {
      playCue(cue)
    }
  }

  fun playVolumePreview(newVolume: Float) {
    volume = newVolume
    playCue(SoundCue.PREVIEW)
  }

  private fun playCue(cue: SoundCue) {
    if (muted || volume <= 0.01f) return
    val pool = soundPool ?: return
    val id = soundIds[cue] ?: return
    if (id !in loadedIds) return
    val v = volume.coerceIn(0f, 1f)
    pool.play(id, v, v, 1, 0, 1f)
  }

  private fun specsFor(cue: SoundCue): List<ToneSpec> = when (cue) {
    SoundCue.CLICK -> listOf(ToneSpec(500.0, 0.035, Wave.SINE, 0.09))
    SoundCue.ERROR -> listOf(ToneSpec(130.0, 0.18, Wave.SAWTOOTH, 0.09))
    SoundCue.PREVIEW -> listOf(ToneSpec(440.0, 0.05, Wave.SINE, 0.14))
    SoundCue.MOVE -> listOf(ToneSpec(300.0, 0.07, Wave.TRIANGLE, 0.20))
    SoundCue.CAPTURE -> listOf(
      ToneSpec(180.0, 0.10, Wave.SQUARE, 0.12, 0.0),
      ToneSpec(110.0, 0.14, Wave.TRIANGLE, 0.20, 0.0),
    )
    SoundCue.CHECK -> listOf(
      ToneSpec(660.0, 0.10, Wave.SINE, 0.18, 0.0),
      ToneSpec(880.0, 0.14, Wave.SINE, 0.18, 0.10),
    )
    SoundCue.CHECKMATE -> listOf(
      ToneSpec(440.0, 0.15, Wave.SINE, 0.18, 0.0),
      ToneSpec(330.0, 0.15, Wave.SINE, 0.18, 0.15),
      ToneSpec(220.0, 0.30, Wave.SINE, 0.18, 0.30),
    )
    SoundCue.CASTLE -> listOf(
      ToneSpec(260.0, 0.06, Wave.TRIANGLE, 0.18, 0.0),
      ToneSpec(340.0, 0.08, Wave.TRIANGLE, 0.18, 0.09),
    )
    SoundCue.PROMOTE -> listOf(
      ToneSpec(523.0, 0.08, Wave.SINE, 0.18, 0.0),
      ToneSpec(784.0, 0.14, Wave.SINE, 0.18, 0.08),
    )
    SoundCue.SUCCESS -> listOf(
      ToneSpec(523.0, 0.10, Wave.SINE, 0.18, 0.0),
      ToneSpec(659.0, 0.10, Wave.SINE, 0.18, 0.10),
      ToneSpec(784.0, 0.22, Wave.SINE, 0.18, 0.20),
    )
  }

  private fun writeCueWav(cue: SoundCue, outFile: File) {
    val sampleRate = 22050
    val specs = specsFor(cue)
    val totalSec = specs.maxOf { it.offsetSec + it.durationSec }
    val totalSamples = (totalSec * sampleRate).toInt().coerceAtLeast(1)
    val mix = DoubleArray(totalSamples)

    for (spec in specs) {
      val startSample = (spec.offsetSec * sampleRate).toInt()
      val noteSamples = (spec.durationSec * sampleRate).toInt().coerceAtLeast(1)
      val decayRate = 6.9 / noteSamples
      for (i in 0 until noteSamples) {
        val idx = startSample + i
        if (idx >= totalSamples) break
        val phase = (i * spec.freqHz / sampleRate) % 1.0
        val raw = when (spec.wave) {
          Wave.SINE -> sin(2.0 * PI * phase)
          Wave.TRIANGLE -> 4.0 * abs(phase - 0.5) - 1.0
          Wave.SQUARE -> if (phase < 0.5) 1.0 else -1.0
          Wave.SAWTOOTH -> 2.0 * phase - 1.0
        }
        mix[idx] += raw * exp(-decayRate * i) * spec.gain
      }
    }

    val pcmBytes = ByteArray(totalSamples * 2)
    for (i in 0 until totalSamples) {
      val sample = (mix[i] * Short.MAX_VALUE)
        .toInt()
        .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
        .toShort()
      pcmBytes[i * 2] = (sample.toInt() and 0xFF).toByte()
      pcmBytes[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
    }

    val header = wavHeader(pcmBytes.size, sampleRate)
    FileOutputStream(outFile).use { fos ->
      fos.write(header)
      fos.write(pcmBytes)
    }
  }

  private fun wavHeader(dataSize: Int, sampleRate: Int): ByteArray {
    val totalDataLen = dataSize + 36
    val byteRate = sampleRate * 2
    return byteArrayOf(
      'R'.code.toByte(), 'I'.code.toByte(), 'F'.code.toByte(), 'F'.code.toByte(),
      (totalDataLen and 0xff).toByte(),
      ((totalDataLen shr 8) and 0xff).toByte(),
      ((totalDataLen shr 16) and 0xff).toByte(),
      ((totalDataLen shr 24) and 0xff).toByte(),
      'W'.code.toByte(), 'A'.code.toByte(), 'V'.code.toByte(), 'E'.code.toByte(),
      'f'.code.toByte(), 'm'.code.toByte(), 't'.code.toByte(), ' '.code.toByte(),
      16, 0, 0, 0, // Subchunk1Size (16 for PCM)
      1, 0, // AudioFormat (1 = PCM)
      1, 0, // NumChannels (1 = Mono)
      (sampleRate and 0xff).toByte(),
      ((sampleRate shr 8) and 0xff).toByte(),
      ((sampleRate shr 16) and 0xff).toByte(),
      ((sampleRate shr 24) and 0xff).toByte(),
      (byteRate and 0xff).toByte(),
      ((byteRate shr 8) and 0xff).toByte(),
      ((byteRate shr 16) and 0xff).toByte(),
      ((byteRate shr 24) and 0xff).toByte(),
      2, 0, // BlockAlign
      16, 0, // BitsPerSample
      'd'.code.toByte(), 'a'.code.toByte(), 't'.code.toByte(), 'a'.code.toByte(),
      (dataSize and 0xff).toByte(),
      ((dataSize shr 8) and 0xff).toByte(),
      ((dataSize shr 16) and 0xff).toByte(),
      ((dataSize shr 24) and 0xff).toByte(),
    )
  }

  fun release() {
    soundPool?.release()
    soundPool = null
  }
}
