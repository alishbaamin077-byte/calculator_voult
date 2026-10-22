package com.example.calc

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.sqrt

class CalculatorEngine {

    var isOn = true
        private set
    var hasError = false
        private set
    var displayText = "0."
        private set
    val hasMemory: Boolean get() = memory != 0.0

    private var memory = 0.0
    private var entry = "0"
    private var typing = false
    private var awaitingOperand = false
    private var acc = 0.0
    private var pendingOp: Char? = null
    private var repeatOp: Char? = null
    private var repeatOperand = 0.0
    private var lastWasClear = false
    private var lastWasMrc = false

    fun press(key: String) {
        if (!isOn) {                       // any key wakes it after OFF
            if (key != "off") { isOn = true; resetAll(); refresh() }
            return
        }
        if (key == "off") {
            isOn = false; memory = 0.0; resetAll(); refresh()
            return
        }
        if (hasError && key != "c") return

        val wasClear = lastWasClear
        val wasMrc = lastWasMrc
        lastWasClear = false
        lastWasMrc = false
        if (key != "=") repeatOp = null

        when {
            key.length == 1 && key[0].isDigit() -> digit(key)
            key == "." -> dot()
            key == "+" || key == "-" || key == "*" || key == "/" -> operator(key[0])
            key == "=" -> equals()
            key == "%" -> percent()
            key == "sqrt" -> squareRoot()
            key == "neg" -> negate()
            key == "c" -> clear(wasClear)
            key == "mrc" -> memoryRecall(wasMrc)
            key == "m+" -> memoryAdd(1.0)
            key == "m-" -> memoryAdd(-1.0)
        }
        refresh()
    }

    private fun digitCount() = entry.count { it.isDigit() }

    private fun digit(d: String) {
        if (!typing) { entry = "0"; typing = true; awaitingOperand = false }
        if (digitCount() >= MAX_DIGITS) return
        entry = if (entry == "0") d else entry + d
    }

    private fun dot() {
        if (!typing) { entry = "0."; typing = true; awaitingOperand = false; return }
        if ('.' !in entry && digitCount() < MAX_DIGITS) entry += "."
    }

    private fun negate() {
        if (typing) {
            entry = when {
                entry.startsWith("-") -> entry.drop(1)
                entry == "0" -> entry
                else -> "-$entry"
            }
        } else {
            entry = format(-currentValue()) ?: return fail()
            awaitingOperand = false
        }
    }

    private fun currentValue(): Double = entry.toDouble()

    private fun operator(op: Char) {
        val cur = currentValue()
        if (pendingOp != null && !awaitingOperand) {
            acc = compute(acc, cur, pendingOp!!) ?: return fail()
        } else if (pendingOp == null) {
            acc = cur
        }
        pendingOp = op
        awaitingOperand = true
        typing = false
        entry = format(acc) ?: return fail()
    }

    private fun equals() {
        val op = pendingOp
        if (op != null) {
            val operand = if (awaitingOperand) acc else currentValue()
            val r = compute(acc, operand, op) ?: return fail()
            repeatOp = op
            repeatOperand = operand
            setResult(r)
        } else if (repeatOp != null) {
            val r = compute(currentValue(), repeatOperand, repeatOp!!) ?: return fail()
            setResult(r)
        }
    }

    private fun setResult(r: Double) {
        entry = format(r) ?: return fail()
        typing = false
        awaitingOperand = false
        pendingOp = null
        acc = 0.0
    }

    private fun compute(a: Double, b: Double, op: Char): Double? = when (op) {
        '+' -> a + b
        '-' -> a - b
        '*' -> a * b
        '/' -> if (b == 0.0) null else a / b
        else -> null
    }

    private fun percent() {
        val cur = currentValue()
        val op = pendingOp
        val r: Double = when (op) {
            '+' -> acc + acc * cur / 100
            '-' -> acc - acc * cur / 100
            '*' -> acc * cur / 100
            '/' -> if (cur == 0.0) return fail() else acc / cur * 100
            else -> cur / 100
        }
        if (op != null) setResult(r) else {
            entry = format(r) ?: return fail()
            typing = false
        }
    }

    private fun squareRoot() {
        val v = currentValue()
        if (v < 0) return fail()
        entry = format(sqrt(v)) ?: return fail()
        typing = false
        awaitingOperand = false
    }

    private fun clear(wasClear: Boolean) {
        if (hasError || wasClear || entry == "0") {
            resetAll()
        } else {
            entry = "0"
            typing = false
            lastWasClear = true
        }
    }

    private fun resetAll() {
        entry = "0"; typing = false; awaitingOperand = false
        acc = 0.0; pendingOp = null; repeatOp = null
        hasError = false; lastWasClear = false; lastWasMrc = false
    }

    private fun memoryRecall(wasMrc: Boolean) {
        if (wasMrc) {
            memory = 0.0
        } else {
            entry = format(memory) ?: return fail()
            typing = false
            awaitingOperand = false
            lastWasMrc = true
        }
    }

    private fun memoryAdd(sign: Double) {
        val v = if (awaitingOperand) acc else currentValue()
        memory = BigDecimal(memory + sign * v).setScale(MAX_DIGITS, RoundingMode.HALF_UP).toDouble()
        typing = false
    }

    private fun fail() {
        hasError = true
        pendingOp = null
        repeatOp = null
    }

    private fun refresh() {
        displayText = when {
            hasError -> "E"
            '.' in entry -> entry
            else -> "$entry."
        }
    }

    // Rounds to what an 8-digit display can show; null = overflow
    private fun format(v: Double): String? {
        if (v.isNaN() || v.isInfinite()) return null
        val abs = BigDecimal(Math.abs(v))
        val intLen = if (abs < BigDecimal.ONE) 1 else abs.toBigInteger().toString().length
        if (intLen > MAX_DIGITS) return null
        val bd = BigDecimal(v).setScale(MAX_DIGITS - intLen, RoundingMode.HALF_UP)
        if (bd.abs().toBigInteger().toString().length > MAX_DIGITS) return null
        if (bd.signum() == 0) return "0"
        return bd.stripTrailingZeros().toPlainString()
    }

    companion object { const val MAX_DIGITS = 8 }
}