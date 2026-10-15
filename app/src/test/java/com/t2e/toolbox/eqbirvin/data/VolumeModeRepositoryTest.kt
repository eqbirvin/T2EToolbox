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

    @Test
    fun testSilentModeBehaviorProperties() {
        assertEquals("Silent (Mute only)", SilentModeBehavior.MUTE.label)
        assertEquals("Do Not Disturb (DND)", SilentModeBehavior.DND.label)
        assertEquals(SilentModeBehavior.MUTE, SilentModeBehavior.valueOf("MUTE"))
        assertEquals(SilentModeBehavior.DND, SilentModeBehavior.valueOf("DND"))
    }

    @Test
    fun testRingerCycleResultDisplayLabel() {
        val resultDefault = RingerCycleResult.Success(RingerModeState.NORMAL, RingerModeState.VIBRATE)
        assertEquals("Vibrate", resultDefault.displayLabel)

        val resultCustomDnd = RingerCycleResult.Success(RingerModeState.VIBRATE, RingerModeState.SILENT, "Do Not Disturb")
        assertEquals("Do Not Disturb", resultCustomDnd.displayLabel)

        val resultCustomMute = RingerCycleResult.Success(RingerModeState.VIBRATE, RingerModeState.SILENT, "Silent")
        assertEquals("Silent", resultCustomMute.displayLabel)
    }

    @Test
    fun testShizukuShellQuote() {
        assertEquals("'hello'", ShizukuShell.shellQuote("hello"))
        assertEquals("'don'\\''t'", ShizukuShell.shellQuote("don't"))
        assertEquals("'foo bar'", ShizukuShell.shellQuote("foo bar"))
    }
}
