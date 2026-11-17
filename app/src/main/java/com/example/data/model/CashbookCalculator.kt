package com.example.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class CalculatorState(
    val expression: String = "",
    val memory: Double = 0.0,
    val lastEvaluated: Double = 0.0
)

object CashbookCalculator {
    private val operators = setOf('+', '-', '×', '÷', '*', '/')

    fun appendDigit(expression: String, digit: Char): String {
        if (!digit.isDigit()) return expression
        if (expression.length >= 28) return expression
        val currentToken = currentNumberToken(expression)
        if (currentToken == "0") {
            return expression.dropLast(1) + digit
        }
        return expression + digit
    }

    fun appendDecimal(expression: String): String {
        if (expression.length >= 28) return expression
        val currentToken = currentNumberToken(expression)
        if (currentToken.contains('.')) return expression
        return if (currentToken.isEmpty()) {
            expression + "0."
        } else {
            "$expression."
        }
    }

    fun appendOperator(expression: String, op: Char): String {
        val normalizedOp = when (op) {
            '*' -> '×'
            '/' -> '÷'
            else -> op
        }
        if (normalizedOp !in operators) return expression
        if (expression.isEmpty()) return expression
        val last = expression.last()
        return when {
            last in operators -> expression.dropLast(1) + normalizedOp
            last == '.' -> expression + "0" + normalizedOp
            else -> expression + normalizedOp
        }
    }

    fun applyPercent(expression: String): String {
        if (expression.isBlank()) return expression
        // Find last operator; if A op B%, compute B as (A * B / 100) for + / -, or B / 100 for × / ÷
        val lastOpIndex = expression.indexOfLast { it in operators }
        if (lastOpIndex <= 0) {
            val valNum = expression.toDoubleOrNull() ?: return expression
            return formatNumber(valNum / 100.0)
        }
        val leftExpr = expression.substring(0, lastOpIndex)
        val opChar = expression[lastOpIndex]
        val rightExpr = expression.substring(lastOpIndex + 1)
        val rightVal = rightExpr.toDoubleOrNull() ?: return expression
        val leftVal = evaluate(leftExpr) ?: return expression
        val pctVal = if (opChar == '+' || opChar == '-') {
            leftVal * (rightVal / 100.0)
        } else {
            rightVal / 100.0
        }
        return formatNumber(evaluateBinary(leftVal, opChar, pctVal) ?: return expression)
    }

    fun backspace(expression: String): String {
        return if (expression.isEmpty()) "" else expression.dropLast(1)
    }

    fun evaluate(expression: String): Double? {
        val cleaned = expression.trim()
            .replace('×', '*')
            .replace('÷', '/')
            .trimEnd('+', '-', '*', '/', '.')
        if (cleaned.isBlank()) return null

        return try {
            val tokens = tokenize(cleaned)
            if (tokens.isEmpty()) return null
            val rpn = toRpn(tokens)
            val result = evalRpn(rpn)
            if (result.isNaN() || result.isInfinite()) null else result
        } catch (_: Exception) {
            null
        }
    }

    fun evaluateToAmount(expression: String): Double {
        return (evaluate(expression) ?: 0.0).coerceAtLeast(0.0)
    }

    fun evaluateOrKeep(expression: String): String {
        val result = evaluate(expression) ?: return expression
        return formatNumber(result)
    }

    fun formatNumber(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val rounded = if (kotlin.math.abs(value - kotlin.math.round(value)) < 1e-9) {
            kotlin.math.round(value).toLong().toString()
        } else {
            val df = DecimalFormat("0.##", DecimalFormatSymbols(Locale.US))
            df.format(value)
        }
        return rounded
    }

    private fun currentNumberToken(expression: String): String {
        val idx = expression.indexOfLast { it in operators }
        return if (idx == -1) expression else expression.substring(idx + 1)
    }

    private fun evaluateBinary(a: Double, op: Char, b: Double): Double? {
        return when (op) {
            '+' -> a + b
            '-' -> a - b
            '×', '*' -> a * b
            '÷', '/' -> if (kotlin.math.abs(b) < 1e-12) null else a / b
            else -> null
        }
    }

    private fun tokenize(expr: String): List<String> {
        val out = mutableListOf<String>()
        var i = 0
        while (i < expr.length) {
            val ch = expr[i]
            when {
                ch.isWhitespace() -> i++
                ch == '-' && (out.isEmpty() || out.last() in listOf("+", "-", "*", "/")) -> {
                    val sb = StringBuilder("-")
                    i++
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    if (sb.length > 1) out.add(sb.toString())
                }
                ch.isDigit() || ch == '.' -> {
                    val sb = StringBuilder()
                    while (i < expr.length && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    out.add(sb.toString())
                }
                ch in listOf('+', '-', '*', '/') -> {
                    out.add(ch.toString())
                    i++
                }
                else -> i++
            }
        }
        return out
    }

    private fun precedence(op: String): Int = when (op) {
        "*", "/" -> 2
        "+", "-" -> 1
        else -> 0
    }

    private fun toRpn(tokens: List<String>): List<String> {
        val output = mutableListOf<String>()
        val ops = ArrayDeque<String>()
        for (token in tokens) {
            if (token in listOf("+", "-", "*", "/")) {
                while (ops.isNotEmpty() && precedence(ops.last()) >= precedence(token)) {
                    output.add(ops.removeLast())
                }
                ops.addLast(token)
            } else {
                output.add(token)
            }
        }
        while (ops.isNotEmpty()) {
            output.add(ops.removeLast())
        }
        return output
    }

    private fun evalRpn(rpn: List<String>): Double {
        val stack = ArrayDeque<Double>()
        for (token in rpn) {
            when (token) {
                "+" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.addLast(a + b)
                }
                "-" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.addLast(a - b)
                }
                "*" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    stack.addLast(a * b)
                }
                "/" -> {
                    val b = stack.removeLast()
                    val a = stack.removeLast()
                    if (kotlin.math.abs(b) < 1e-12) throw ArithmeticException("Division by zero")
                    stack.addLast(a / b)
                }
                else -> stack.addLast(token.toDouble())
            }
        }
        return stack.last()
    }
}
