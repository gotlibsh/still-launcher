package com.gotlibsh.still.search

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * A tiny calculator for the search box: + - * / ^, brackets, decimals, and % (as "per cent",
 * so 150*20% = 30). Pure Kotlin so it can be unit tested.
 */
object Calculator {

    /** The answer as text ("84", "0.5"), or null if [text] isn't a sum worth answering. */
    fun answer(text: String): String? {
        val s = text.replace('×', '*').replace('x', '*').replace('X', '*')
            .replace('÷', '/').replace(',', '.').filterNot { it.isWhitespace() }
        if (s.isEmpty() || s.any { it !in "0123456789.+-*/^()%" }) return null
        // A plain number ("2048", "-5") is not a sum; leave it to app search.
        if (s.drop(1).none { it in "+-*/^%" }) return null
        val value = try {
            val parser = Parser(s)
            val v = parser.expression()
            if (parser.pos != s.length) return null
            v
        } catch (e: Exception) {
            return null
        }
        if (value.isNaN() || value.isInfinite()) return null
        return format(value)
    }

    private fun format(value: Double): String {
        val rounded = BigDecimal(value).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros()
        return if (rounded.signum() == 0) "0" else rounded.toPlainString()
    }

    private class Parser(val s: String) {
        var pos = 0

        fun expression(): Double {
            var v = term()
            while (pos < s.length && (s[pos] == '+' || s[pos] == '-')) {
                val op = s[pos++]
                val r = term()
                v = if (op == '+') v + r else v - r
            }
            return v
        }

        private fun term(): Double {
            var v = power()
            while (pos < s.length && (s[pos] == '*' || s[pos] == '/')) {
                val op = s[pos++]
                val r = power()
                v = if (op == '*') v * r else v / r
            }
            return v
        }

        private fun power(): Double {
            val base = unary()
            if (pos < s.length && s[pos] == '^') {
                pos++
                return Math.pow(base, power()) // right-associative
            }
            return base
        }

        private fun unary(): Double {
            if (pos < s.length && s[pos] == '-') { pos++; return -unary() }
            if (pos < s.length && s[pos] == '+') { pos++; return unary() }
            return percent()
        }

        private fun percent(): Double {
            var v = atom()
            while (pos < s.length && s[pos] == '%') { pos++; v /= 100.0 }
            return v
        }

        private fun atom(): Double {
            if (pos < s.length && s[pos] == '(') {
                pos++
                val v = expression()
                require(pos < s.length && s[pos] == ')')
                pos++
                return v
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            require(pos > start)
            return s.substring(start, pos).toDouble()
        }
    }
}
