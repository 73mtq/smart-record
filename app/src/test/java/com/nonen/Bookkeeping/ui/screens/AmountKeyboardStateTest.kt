package com.nonen.Bookkeeping.ui.screens

import org.junit.Assert.*
import org.junit.Test

class AmountKeyboardStateTest {
    @Test fun `decimal entry allows two places and leading decimal point`() {
        val state = ".125".fold(AmountKeyboardState()) { value, digit -> value.digit(digit) }
        assertEquals("0.12", state.operand)
        assertTrue(state.canSave)
    }

    @Test fun `operator evaluates preceding expression before next operation`() {
        val state = AmountKeyboardState("12").operate(AmountOperator.ADD).digit('3').operate(AmountOperator.MULTIPLY).digit('2').evaluate()
        assertEquals("30", state.operand)
        assertEquals("7", state.digit('7').operand)
    }

    @Test fun `backspace removes pending operator and zero divisor cannot evaluate`() {
        assertEquals("12", AmountKeyboardState("12").operate(AmountOperator.ADD).backspace().operand)
        val divided = AmountKeyboardState("12").operate(AmountOperator.DIVIDE).digit('0')
        assertTrue(divided.hasZeroDivisor)
        assertFalse(divided.canEvaluate)
    }

    @Test fun `division rounds amounts and input limit is enforced`() {
        assertEquals("0.33", AmountKeyboardState("1").operate(AmountOperator.DIVIDE).digit('3').evaluate().operand)
        assertEquals("1000000000", AmountKeyboardState("1000000000").digit('1').operand)
    }
}
