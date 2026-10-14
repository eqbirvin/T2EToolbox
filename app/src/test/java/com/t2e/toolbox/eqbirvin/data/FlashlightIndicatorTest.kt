package com.t2e.toolbox.eqbirvin.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class FlashlightIndicatorTest {

    @Test
    fun testFlashlightIndicatorColorProperties() {
        assertEquals(0xFFFFB300, FlashlightIndicatorColor.AMBER.colorArgb)
        assertEquals("Amber Glow", FlashlightIndicatorColor.AMBER.label)

        assertEquals(0xFFFFEB3B, FlashlightIndicatorColor.YELLOW.colorArgb)
        assertEquals("Bright Yellow", FlashlightIndicatorColor.YELLOW.label)

        assertEquals(0xFFFFFFFF, FlashlightIndicatorColor.WHITE.colorArgb)
        assertEquals("Crisp White", FlashlightIndicatorColor.WHITE.label)
    }

    @Test
    fun testFlashlightIndicatorColorSerializationNames() {
        assertEquals(FlashlightIndicatorColor.AMBER, FlashlightIndicatorColor.valueOf("AMBER"))
        assertEquals(FlashlightIndicatorColor.YELLOW, FlashlightIndicatorColor.valueOf("YELLOW"))
        assertEquals(FlashlightIndicatorColor.WHITE, FlashlightIndicatorColor.valueOf("WHITE"))
    }

    @Test
    fun testFlashlightIndicatorColorValuesLength() {
        assertEquals(3, FlashlightIndicatorColor.values().size)
    }
}
