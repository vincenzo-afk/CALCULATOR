package com.example.advancedcalculator

import kotlin.math.*

/**
 * Safe math expression evaluator built with a recursive-descent parser.
 * Replaces the previous Rhino JS eval() which allowed arbitrary code execution.
 *
 * Supports:
 *  + - * / % (modulus)  ^ (power)
 *  Unary minus, implicit multiplication (e.g. 2(3+1), 2pi)
 *  Parentheses
 *  Constants: pi, e, tau
 *  Functions: sin, cos, tan, asin, acos, atan, log (base 10), ln, sqrt, cbrt,
 *             abs, ceil, floor, round, fact (! as postfix)
 *  Angles for trig functions can be DEGREE or RADIAN (default DEGREE like the
 *  Google Calculator app).
 */
class ExpressionEvaluator(private val angleUnit: AngleUnit = AngleUnit.DEGREE) {

    enum class AngleUnit { DEGREE, RADIAN }

    sealed class EvalError(message: String) : RuntimeException(message) {
        object DivisionByZero : EvalError("Division by zero")
        object InvalidSyntax : EvalError("Invalid expression")
        object DomainError : EvalError("Math domain error")
        object Overflow : EvalError("Result too large")
        class Unknown(message: String) : EvalError(message)
    }

    private var pos = 0
    private lateinit var tokens: List<Token>

    fun evaluate(input: String): Double {
        val cleaned = input
            .replace("×", "*").replace("÷", "/").replace("−", "-")
            .replace("π", "pi").replace("√", "sqrt")
            .replace(" ", "")
            .lowercase()
        if (cleaned.isEmpty()) throw EvalError.InvalidSyntax
        tokens = tokenize(cleaned)
        if (tokens.isEmpty()) throw EvalError.InvalidSyntax
        pos = 0
        val result = parseExpression()
        if (pos < tokens.size) throw EvalError.InvalidSyntax
        return result
    }

    private fun toRadians(value: Double): Double =
        if (angleUnit == AngleUnit.DEGREE) Math.toRadians(value) else value

    private fun factorial(n: Int): Double {
        if (n < 0) throw EvalError.DomainError
        var result = 1.0
        for (i in 2..n) {
            result *= i
            if (result.isInfinite()) throw EvalError.Overflow
        }
        return result
    }

    // --- Tokenizer ---
    private sealed class Token
    private data class NumberToken(val value: Double) : Token()
    private data class OpToken(val op: String) : Token()
    private data class FuncToken(val name: String) : Token()
    private data class ConstToken(val name: String) : Token()
    private class LParenToken : Token()
    private class RParenToken : Token()
    private class BangToken : Token() // postfix factorial

    private fun tokenize(input: String): List<Token> {
        val out = mutableListOf<Token>()
        var i = 0
        var prevWasValue = false // true after a number, const, ), !
        while (i < input.length) {
            val ch = input[i]
            when {
                ch.isDigit() || ch == '.' -> {
                    val start = i
                    while (i < input.length && (input[i].isDigit() || input[i] == '.')) i++
                    if (prevWasValue) out.add(OpToken("*"))
                    out.add(NumberToken(input.substring(start, i).toDoubleOrNull() ?: throw EvalError.InvalidSyntax))
                    prevWasValue = true
                }
                ch.isLetter() -> {
                    val start = i
                    while (i < input.length && input[i].isLetter()) i++
                    val word = input.substring(start, i)
                    val isFunction = word in FUNCTIONS
                    // implicit multiplication: 2pi, 2sin(5), 2(3+1)
                    if (prevWasValue && !isFunction) out.add(OpToken("*"))
                    when {
                        word == "pi" || word == "e" || word == "tau" -> out.add(ConstToken(word))
                        isFunction -> out.add(FuncToken(word))
                        else -> throw EvalError.InvalidSyntax
                    }
                    // constants behave like values (2pi = 2*pi) but functions are
                    // immediately followed by '(' so they must NOT look like values:
                    // otherwise "sin(" would emit FuncToken + '*' + '('.
                    prevWasValue = word == "pi" || word == "e" || word == "tau"
                }
                ch == ')' -> {
                    out.add(RParenToken()); i++; prevWasValue = true
                }
                ch == '!' -> {
                    out.add(BangToken()); i++
                }
                ch == '(' -> {
                    if (prevWasValue) out.add(OpToken("*"))
                    out.add(LParenToken()); i++; prevWasValue = false
                }
                ch == '-' && !prevWasValue -> {
                    // prefix minus: the parser handles it in parseUnary
                    out.add(OpToken("-")); i++; prevWasValue = false
                }
                "+-*/%^".contains(ch) -> {
                    out.add(OpToken(ch.toString())); i++; prevWasValue = false
                }
                else -> throw EvalError.InvalidSyntax
            }
        }
        return out
    }

    companion object {
        val FUNCTIONS = setOf("sin", "cos", "tan", "asin", "acos", "atan", "log", "ln", "sqrt", "cbrt", "abs", "ceil", "floor", "round")
    }

    // --- Recursive descent parser ---
    // grammar: expr    = term (('+' | '-') term)*
    //          term   = power (('*' | '/' | '%') power)*
    //          power  = unary ('^' unary)*                  (right-associative)
    //          unary  = ('-') unary | postfix ('!')*
    //          postfix = number | const | func '(' expr ')' | '(' expr ')'

    @Suppress("DuplicatedCode")
    private fun parseExpression(): Double {
        var result = parseTerm()
        while (pos < tokens.size && tokens[pos] is OpToken && (tokens[pos] as OpToken).op in "+-") {
            val op = (tokens[pos++] as OpToken).op
            val right = parseTerm()
            result = if (op == "+") result + right else result - right
        }
        return result
    }

    private fun parseTerm(): Double {
        var result = parsePower()
        while (pos < tokens.size && tokens[pos] is OpToken && (tokens[pos] as OpToken).op in "*/%") {
            val op = (tokens[pos++] as OpToken).op
            val right = parsePower()
            result = when (op) {
                "*" -> result * right
                "/" -> if (right == 0.0) throw EvalError.DivisionByZero else result / right
                "%" -> if (right == 0.0) throw EvalError.DivisionByZero else result % right
                else -> result
            }
        }
        return result
    }

    private fun parsePower(): Double {
        val base = parseUnary()
        if (pos < tokens.size && tokens[pos] is OpToken && (tokens[pos] as OpToken).op == "^") {
            pos++
            val exp = parsePower() // right-associative
            return base.pow(exp)
        }
        return base
    }

    private fun parseUnary(): Double {
        // grammar: unary = ('-') unary | postfix ('!')*
        if (pos < tokens.size && tokens[pos] is OpToken && (tokens[pos] as OpToken).op == "-") {
            pos++
            return -parseUnary()
        }
        var result = parsePostfix()
        while (pos < tokens.size && tokens[pos] is BangToken) {
            pos++
            val n = result.toLong()
            if (n.toDouble() != result) throw EvalError.InvalidSyntax
            result = factorial(n.toInt())
        }
        return result
    }

    private fun parsePostfix(): Double {
        return when (val t = if (pos < tokens.size) tokens[pos] else null) {
            is NumberToken -> {
                pos++
                t.value
            }
            is ConstToken -> {
                pos++
                when (t.name) {
                    "pi" -> PI
                    "e" -> E
                    "tau" -> 2 * PI
                    else -> throw EvalError.InvalidSyntax
                }
            }
            is FuncToken -> {
                pos++
                // skip an implicit '*' inserted between a value and the function (e.g. 3sin(...))
                if (pos < tokens.size && tokens[pos] is OpToken && (tokens[pos] as OpToken).op == "*") pos++
                if (pos >= tokens.size || tokens[pos] !is LParenToken) throw EvalError.InvalidSyntax
                pos++ // consume '('
                val arg = parseExpression()
                if (pos >= tokens.size || tokens[pos] !is RParenToken) throw EvalError.InvalidSyntax
                pos++ // consume ')'
                applyFunction(t.name, arg)
            }
            is LParenToken -> {
                pos++ // consume '('
                val value = parseExpression()
                if (pos >= tokens.size || tokens[pos] !is RParenToken) throw EvalError.InvalidSyntax
                pos++ // consume ')'
                value
            }
            else -> throw EvalError.InvalidSyntax
        }
    }

    private fun applyFunction(name: String, arg: Double): Double = when (name) {
        "sin" -> sin(toRadians(arg))
        "cos" -> cos(toRadians(arg))
        "tan" -> tan(toRadians(arg))
        "asin" -> {
            if (arg < -1.0 || arg > 1.0) throw EvalError.DomainError
            Math.toDegrees(asin(arg)) // return in degrees regardless of mode
        }
        "acos" -> {
            if (arg < -1.0 || arg > 1.0) throw EvalError.DomainError
            Math.toDegrees(acos(arg))
        }
        "atan" -> Math.toDegrees(atan(arg))
        "log" -> if (arg <= 0.0) throw EvalError.DomainError else log10(arg)
        "ln" -> if (arg <= 0.0) throw EvalError.DomainError else ln(arg)
        "sqrt" -> if (arg < 0.0) throw EvalError.DomainError else sqrt(arg)
        "cbrt" -> cbrt(arg)
        "abs" -> abs(arg)
        "ceil" -> ceil(arg)
        "floor" -> floor(arg)
        "round" -> round(arg)
        else -> throw EvalError.InvalidSyntax
    }
}

/** Format a result for display: up to 10 significant digits, no trailing zeros. */
fun formatResult(value: Double): String {
    return if (value.isNaN()) "NaN"
    else if (value.isInfinite()) "Error"
    else {
        val rounded = kotlin.math.round(value * 1e10) / 1e10
        val s = rounded.toString()
        if (s.contains('E') || s.contains('e')) {
            String.format("%.8g", rounded)
        } else {
            s
        }
    }
}
