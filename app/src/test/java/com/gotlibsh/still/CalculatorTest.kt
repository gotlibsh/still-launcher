package com.gotlibsh.still

import com.gotlibsh.still.search.Calculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculatorTest {
    @Test fun basicSums() {
        assertEquals("84", Calculator.answer("12*7"))
        assertEquals("84", Calculator.answer("12 x 7"))
        assertEquals("14", Calculator.answer("2+3*4"))
        assertEquals("20", Calculator.answer("(2+3)*4"))
        assertEquals("0.5", Calculator.answer("1/2"))
        assertEquals("2.5", Calculator.answer("1,5+1"))
        assertEquals("-3", Calculator.answer("-5+2"))
        assertEquals("0.3", Calculator.answer("0.1+0.2"))
    }

    @Test fun powersAndPercent() {
        assertEquals("512", Calculator.answer("2^3^2"))
        assertEquals("30", Calculator.answer("150*20%"))
        assertEquals("3.33333333", Calculator.answer("10/3"))
    }

    @Test fun notASum() {
        assertNull(Calculator.answer("2048"))
        assertNull(Calculator.answer("-5"))
        assertNull(Calculator.answer("maps"))
        assertNull(Calculator.answer("12*"))
        assertNull(Calculator.answer("1/0"))
        assertNull(Calculator.answer("(2+3"))
        assertNull(Calculator.answer(""))
        assertNull(Calculator.answer("x"))
    }
}
