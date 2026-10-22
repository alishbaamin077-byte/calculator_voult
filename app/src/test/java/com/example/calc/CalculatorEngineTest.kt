package com.example.calc

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CalculatorEngineTest {

    private lateinit var engine: CalculatorEngine

    @Before
    fun setUp() {
        engine = CalculatorEngine()
    }

    @Test
    fun testInitialState() {
        assertTrue(engine.isOn)
        assertFalse(engine.hasError)
        assertFalse(engine.hasMemory)
        assertEquals("0.", engine.displayText)
    }

    @Test
    fun testBasicAddition() {
        engine.press("2")
        engine.press("+")
        engine.press("3")
        engine.press("=")
        assertEquals("5.", engine.displayText)
    }

    @Test
    fun testSubtraction() {
        engine.press("9")
        engine.press("-")
        engine.press("4")
        engine.press("=")
        assertEquals("5.", engine.displayText)
    }

    @Test
    fun testMultiplication() {
        engine.press("6")
        engine.press("*")
        engine.press("7")
        engine.press("=")
        assertEquals("42.", engine.displayText)
    }

    @Test
    fun testDivision() {
        engine.press("8")
        engine.press("/")
        engine.press("2")
        engine.press("=")
        assertEquals("4.", engine.displayText)
    }

    @Test
    fun testDivisionByZero() {
        engine.press("5")
        engine.press("/")
        engine.press("0")
        engine.press("=")
        assertTrue(engine.hasError)
        assertEquals("E", engine.displayText)
    }

    @Test
    fun testSquareRoot() {
        engine.press("9")
        engine.press("sqrt")
        assertEquals("3.", engine.displayText)
    }

    @Test
    fun testNegativeSquareRootError() {
        engine.press("9")
        engine.press("neg")
        engine.press("sqrt")
        assertTrue(engine.hasError)
        assertEquals("E", engine.displayText)
    }

    @Test
    fun testMemoryOperations() {
        assertFalse(engine.hasMemory)
        engine.press("5")
        engine.press("m+")
        assertTrue(engine.hasMemory)

        engine.press("c")
        engine.press("mrc")
        assertEquals("5.", engine.displayText)

        // recall again clears memory
        engine.press("mrc")
        assertFalse(engine.hasMemory)
    }

    @Test
    fun testPowerOffAndOn() {
        engine.press("5")
        engine.press("off")
        assertFalse(engine.isOn)

        engine.press("3")
        assertTrue(engine.isOn)
        assertEquals("0.", engine.displayText)
    }
}
