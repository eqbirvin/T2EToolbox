package com.t2e.toolbox.eqbirvin.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ShizukuShellTest {

    @Test
    fun testShellQuotePlainString() {
        assertEquals("'hello'", ShizukuShell.shellQuote("hello"))
        assertEquals("'com.android.settings'", ShizukuShell.shellQuote("com.android.settings"))
    }

    @Test
    fun testShellQuoteStringWithDollar() {
        assertEquals("'com.example\$MyService'", ShizukuShell.shellQuote("com.example\$MyService"))
        assertEquals("'\$PATH'", ShizukuShell.shellQuote("\$PATH"))
    }

    @Test
    fun testShellQuoteStringWithSingleQuote() {
        assertEquals("'it'\\''s'", ShizukuShell.shellQuote("it's"))
        assertEquals("'foo'\\''bar'\\''baz'", ShizukuShell.shellQuote("foo'bar'baz"))
    }

    @Test
    fun testShellQuoteEmptyString() {
        assertEquals("''", ShizukuShell.shellQuote(""))
    }

    @Test
    fun testShellQuoteMixedComplexString() {
        assertEquals("'\$var'\\''s_test'", ShizukuShell.shellQuote("\$var's_test"))
    }
}
