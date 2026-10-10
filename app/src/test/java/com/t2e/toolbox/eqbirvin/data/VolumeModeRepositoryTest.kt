package com.t2e.toolbox.eqbirvin.data

import org.junit.Assert.assertEquals
import org.junit.Test

class VolumeModeRepositoryTest {

    @Test
    fun testAllModesCycleSequence() {
        // Normal -> Vibrate
        val nextFromNormal = calculateNextRingerMode(RingerModeState.NORMAL, VolumeCycleOption.ALL_MODES)
        assertEquals(RingerModeState.VIBRATE, nextFromNormal)

        // Vibrate -> Silent
        val nextFromVibrate = calculateNextRingerMode(RingerModeState.VIBRATE, VolumeCycleOption.ALL_MODES)
        assertEquals(RingerModeState.SILENT, nextFromVibrate)

        // Silent -> Normal
        val nextFromSilent = calculateNextRingerMode(RingerModeState.SILENT, VolumeCycleOption.ALL_MODES)
        assertEquals(RingerModeState.NORMAL, nextFromSilent)
    }

    @Test
    fun testSoundAndVibrateCycleSequence() {
        // Normal -> Vibrate
        val nextFromNormal = calculateNextRingerMode(RingerModeState.NORMAL, VolumeCycleOption.SOUND_AND_VIBRATE)
        assertEquals(RingerModeState.VIBRATE, nextFromNormal)

        // Vibrate -> Normal
        val nextFromVibrate = calculateNextRingerMode(RingerModeState.VIBRATE, VolumeCycleOption.SOUND_AND_VIBRATE)
        assertEquals(RingerModeState.NORMAL, nextFromVibrate)

        // If currently Silent -> Normal
        val nextFromSilent = calculateNextRingerMode(RingerModeState.SILENT, VolumeCycleOption.SOUND_AND_VIBRATE)
        assertEquals(RingerModeState.NORMAL, nextFromSilent)
    }

    @Test
    fun testSoundAndSilentCycleSequence() {
        // Normal -> Silent
        val nextFromNormal = calculateNextRingerMode(RingerModeState.NORMAL, VolumeCycleOption.SOUND_AND_SILENT)
        assertEquals(RingerModeState.SILENT, nextFromNormal)

        // Silent -> Normal
        val nextFromSilent = calculateNextRingerMode(RingerModeState.SILENT, VolumeCycleOption.SOUND_AND_SILENT)
        assertEquals(RingerModeState.NORMAL, nextFromSilent)

        // If currently Vibrate -> Normal
        val nextFromVibrate = calculateNextRingerMode(RingerModeState.VIBRATE, VolumeCycleOption.SOUND_AND_SILENT)
        assertEquals(RingerModeState.NORMAL, nextFromVibrate)
    }

    @Test
    fun testVibrateAndSilentCycleSequence() {
        // Vibrate -> Silent
        val nextFromVibrate = calculateNextRingerMode(RingerModeState.VIBRATE, VolumeCycleOption.VIBRATE_AND_SILENT)
        assertEquals(RingerModeState.SILENT, nextFromVibrate)

        // Silent -> Vibrate
        val nextFromSilent = calculateNextRingerMode(RingerModeState.SILENT, VolumeCycleOption.VIBRATE_AND_SILENT)
        assertEquals(RingerModeState.VIBRATE, nextFromSilent)

        // If currently Normal -> Vibrate
        val nextFromNormal = calculateNextRingerMode(RingerModeState.NORMAL, VolumeCycleOption.VIBRATE_AND_SILENT)
        assertEquals(RingerModeState.VIBRATE, nextFromNormal)
    }

    @Test
    fun testRingerModeStateFromValue() {
        assertEquals(RingerModeState.NORMAL, RingerModeState.fromValue(2))
        assertEquals(RingerModeState.VIBRATE, RingerModeState.fromValue(1))
        assertEquals(RingerModeState.SILENT, RingerModeState.fromValue(0))
        // Unknown defaults to NORMAL
        assertEquals(RingerModeState.NORMAL, RingerModeState.fromValue(99))
    }
}
