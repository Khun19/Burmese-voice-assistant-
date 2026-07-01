package com.example.engine

import android.util.Log

/**
 * A lightweight, local digital signal processing (DSP) analyzer
 * that acts as a placeholder local audio processing library.
 * It computes Short-Time Energy (RMS) and Zero Crossing Rate (ZCR) 
 * to detect voice/sound triggers in real-time.
 */
class LocalAudioProcessor {
    private val tag = "LocalAudioProcessor"
    private var lastTriggerTime = 0L
    private val triggerCooldownMs = 3000L

    // Adaptive thresholding: background noise estimation
    private var noiseFloorRms = 150.0
    private val alpha = 0.95 // Exponential moving average coefficient

    /**
     * Processes incoming PCM-16bit short buffer.
     * Returns true if a voice/sound wake trigger is detected.
     */
    fun processAudioFrame(shortBuffer: ShortArray, length: Int): Boolean {
        if (length <= 0) return false

        // 1. Calculate Root Mean Square (RMS) energy
        var sumSquares = 0.0
        for (i in 0 until length) {
            val sample = shortBuffer[i].toDouble()
            sumSquares += sample * sample
        }
        val rms = Math.sqrt(sumSquares / length)

        // 2. Calculate Zero Crossing Rate (ZCR) for basic pitch/noise classification
        var zeroCrossings = 0
        for (i in 1 until length) {
            val currentSign = shortBuffer[i] >= 0
            val prevSign = shortBuffer[i - 1] >= 0
            if (currentSign != prevSign) {
                zeroCrossings++
            }
        }
        val zcr = zeroCrossings.toDouble() / length

        // 3. Update background noise floor estimation dynamically during silent/low energy frames
        if (rms < noiseFloorRms * 2) {
            noiseFloorRms = alpha * noiseFloorRms + (1 - alpha) * rms
        }

        // 4. Threshold Logic:
        // Trigger condition: Significant energy spike above the estimated noise floor
        // AND ZCR is in the typical voiced speech spectrum (0.03 to 0.45)
        val energyRatio = rms / noiseFloorRms.coerceAtLeast(10.0)
        val isEnergySpike = energyRatio > 5.5 && rms > 800.0
        val isVoicedZcr = zcr in 0.03..0.45

        val currentTime = System.currentTimeMillis()
        if (isEnergySpike && isVoicedZcr) {
            if (currentTime - lastTriggerTime > triggerCooldownMs) {
                lastTriggerTime = currentTime
                Log.d(
                    tag,
                    "🔥 Local Audio Trigger Fired! RMS: ${String.format("%.1f", rms)} (Noise: ${String.format("%.1f", noiseFloorRms)}), Energy Ratio: ${String.format("%.2f", energyRatio)}, ZCR: ${String.format("%.3f", zcr)}"
                )
                return true
            }
        }

        return false
    }
}
