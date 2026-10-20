package com.nonen.Bookkeeping.ui.screens

import java.math.BigDecimal
import java.math.RoundingMode

enum class AmountOperator(val symbol: String) { ADD("+"), SUBTRACT("-"), MULTIPLY("×"), DIVIDE("÷") }

data class AmountKeyboardState(
    val operand: String = "",
    val left: BigDecimal? = null,
    val operator: AmountOperator? = null,
    val replaceOnDigit: Boolean = false,
) {
    val display: String get() = if (operator == null) operand else "${format(requireNotNull(left))}${operator.symbol}$operand"
    val hasZeroDivisor: Boolean get() = operator == AmountOperator.DIVIDE && operand.toBigDecimalOrNull()?.signum() == 0
    val canEvaluate: Boolean get() = operator != null && operand.isNotEmpty() && !hasZeroDivisor
    val canSave: Boolean get() = operator == null && operand.toBigDecimalOrNull()?.signum() == 1
    val actionLabel: String get() = when { operator != null -> "="; canSave -> "完成"; else -> "取消" }

    fun digit(value: Char): AmountKeyboardState {
        require(value in '0'..'9' || value == '.')
        val current = if (replaceOnDigit) "" else operand
        if (value == '.' && current.contains('.')) return this
        val next = when {
            value == '.' && current.isEmpty() -> "0."
            current == "0" && value != '.' -> value.toString()
            else -> current + value
        }
        if (next.substringAfter('.', "").length > 2) return this
        if (next.toBigDecimal().toLong() > 1_000_000_000L) return this
        return copy(operand = next, replaceOnDigit = false)
    }

    fun backspace(): AmountKeyboardState = when {
        operand.isNotEmpty() -> copy(operand = operand.dropLast(1), replaceOnDigit = false)
        operator != null -> AmountKeyboardState(format(requireNotNull(left)))
        else -> this
    }

    fun operate(next: AmountOperator): AmountKeyboardState {
        if (operator != null && operand.isEmpty()) return copy(operator = next)
        val result = if (operator != null) evaluate() else this
        val number = result.operand.toBigDecimalOrNull() ?: return result
        return AmountKeyboardState(left = number, operator = next)
    }

    fun evaluate(): AmountKeyboardState {
        require(canEvaluate)
        val first = requireNotNull(left)
        val second = operand.toBigDecimal()
        val result = when (requireNotNull(operator)) {
            AmountOperator.ADD -> first + second
            AmountOperator.SUBTRACT -> first - second
            AmountOperator.MULTIPLY -> first * second
            AmountOperator.DIVIDE -> first.divide(second, 2, RoundingMode.HALF_UP)
        }
        return AmountKeyboardState(format(result), replaceOnDigit = true)
    }

    companion object {
        private fun format(value: BigDecimal): String = value.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
    }
}
