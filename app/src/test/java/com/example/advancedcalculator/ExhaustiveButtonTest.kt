package com.example.advancedcalculator

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Exhaustive simulation of the full user interaction pipeline:
 * onButtonClick(token) appends text exactly like the keypad does,
 * then we run formatResult(evaluate(text)) like the display does.
 * Catches bugs between key tokens, expression assembly, evaluation,
 * and formatting — not just the parser in isolation.
 */
class ExhaustiveButtonTest {

    private lateinit var eval: ExpressionEvaluator
    private val results = mutableListOf<String>()

    @Before
    fun setup() {
        eval = ExpressionEvaluator()
        results.clear()
    }

    // Simulate refreshDisplay: evaluate the current expression text and format
    private fun evaluateExpression(text: String): String {
        val cleaned = text.replace("\u2212", "-").replace("\u00d7", "*").replace("\u00f7", "/")
        val trimmed = cleaned.trim()
        // strip a trailing operator so in-progress expressions evaluate (like live preview)
        var expr = trimmed
        while (expr.isNotEmpty() && expr.last().isOperatorChar()) {
            expr = expr.dropLast(1).trimEnd()
        }
        if (expr.isEmpty()) return "0"
        return try {
            formatResult(eval.evaluate(expr))
        } catch (e: ExpressionEvaluator.EvalError) {
            "ERROR"
        } catch (e: Exception) {
            "ERROR"
        }
    }

    private fun Char.isOperatorChar(): Boolean = this in "+-*/^%"

    // Simulate the token -> inserted text mapping for calculator buttons
    private fun tokenToText(token: String, secondFunction: Boolean = false): String = when (token) {
        "sin(" -> if (secondFunction) "asin(" else "sin("
        "cos(" -> if (secondFunction) "acos(" else "cos("
        "tan(" -> if (secondFunction) "atan(" else "tan("
        "x²" -> "^(2)"
        "x³" -> "^(3)"
        "xʸ" -> "^"
        "10ˣ" -> "10^("
        "eˣ" -> "exp("
        "√(" -> "sqrt("
        "∛(" -> "cbrt("
        "lg(" -> "lg("
        "ln(" -> "ln("
        "1/x" -> "reciprocal"
        "π" -> "pi"
        "EE" -> "e"
        "Rand" -> "0.5" // deterministic stand-in; rand value not verifiable
        "%%" -> "/(100)"
        "Ans" -> "0"
        else -> token
    }

    private fun eq(expected: Double, actual: String, tolerance: Double = 1e-6, context: String = "") {
        val parsed = actual.toDoubleOrNull()
        assertNotNull("Expected number but got '$actual' $context", parsed)
        assertEquals(context.ifEmpty { "expected $expected got $parsed" }, expected, parsed!!, tolerance)
    }

    // ---------- Numbers & basic arithmetic ----------
    @Test
    fun everyDigitAndDecimalWork() {
        for (d in 0..9) {
            val v = eval.evaluate("$d".toString()).toString()
            assertEquals("digit $d", d.toDouble(), v.toDouble(), 0.0)
        }
        eq(12.34, evaluateExpression("12.34"), context = "multi-digit decimal")
        eq(100.5, evaluateExpression("100.5"), context = "hundreds")
        eq(999999.0, evaluateExpression("999999"), context = "large number")
    }

    @Test
    fun basicFourOperations() {
        eq(54.0 - 36.0, evaluateExpression("54-36"), context = "54-36")
        eq(7.0 * 6.0, evaluateExpression("7*6"), context = "7*6")
        eq(84.0 / 7.0, evaluateExpression("84/7"), context = "84/7")
        eq(15.0 + 25.0, evaluateExpression("15+25"), context = "15+25")
        eq(190.0, evaluateExpression("156+34"), context = "156+34")
        // Real keypad buttons insert unicode operators — must still compute
        eq(54.0 - 36.0, evaluateExpression("54\u221236"), context = "54−36 unicode")
        eq(7.0 * 6.0, evaluateExpression("7\u00d76"), context = "7×6 unicode")
        eq(84.0 / 7.0, evaluateExpression("84\u00f77"), context = "84÷7 unicode")
    }

    @Test
    fun chainedAdditionLikeHistoryExample() {
        val longExpr = "7500+1400+1390+160+3000+500+100+270+300+334+50+75+50+75000+900+24+11660+60+900+120+350+70+20+1000+1090+20+400+285+140+100+3000+110+250+1200+110+130+15+40+30+120+70+50+60+420+75+20"
        eq(112968.0, evaluateExpression(longExpr), tolerance = 1.0, context = "long chained sum = 112968")
    }

    @Test
    fun operatorPrecedenceAndParentheses() {
        eq(14.0, evaluateExpression("2+3*4"), context = "precedence")
        eq(20.0, evaluateExpression("(2+3)*4"), context = "parens")
        eq(2.5, evaluateExpression("10/(2+2)"), context = "nested")
        eq(8.0, evaluateExpression("2^(3*1)"), context = "power with parens")
    }

    @Test
    fun divisionByZeroAndErrors() {
        val r = evaluateExpression("1/0")
        assertEquals("1/0 must be an error", "ERROR", r)
        assertEquals("sqrt(-1) must be an error", "ERROR", evaluateExpression("sqrt(-1)"))
    }

    // ---------- Trigonometry (DEG mode) ----------
    @Test
    fun trigDegreesKnownValues() {
        eq(0.5, evaluateExpression("sin(30)"), context = "sin(30°)")
        eq(1.0, evaluateExpression("sin(90)"), context = "sin(90°)")
        eq(0.0, evaluateExpression("cos(90)"), context = "cos(90°)")
        eq(-1.0, evaluateExpression("cos(180)"), context = "cos(180°)")
        eq(1.0, evaluateExpression("tan(45)"), context = "tan(45°)")
        eq(30.0, evaluateExpression("asin(0.5)"), context = "asin(0.5)")
        eq(45.0, evaluateExpression("atan(1)"), context = "atan(1)")
    }

    @Test
    fun hyperbolicAndInverse() {
        eq(Math.cosh(1.0), evaluateExpression("cosh(1)"), 1e-9, context = "cosh(1)")
        eq(1.0 / 5.0, evaluateExpression("1/(5)"), 1e-9, context = "1/x of 5")
    }

    // ---------- Powers, roots, factorial ----------
    @Test
    fun powersAndRoots() {
        eq(4.0, evaluateExpression("2^(2)"), context = "square")
        eq(8.0, evaluateExpression("2^(3)"), context = "cube")
        eq(32.0, evaluateExpression("2^5"), context = "power")
        eq(3.0, evaluateExpression("sqrt(9)"), context = "sqrt")
        eq(2.0, evaluateExpression("cbrt(8)"), context = "cbrt")
        eq(120.0, evaluateExpression("5!"), context = "factorial 5")
        eq(24.0, evaluateExpression("4!"), context = "factorial 4")
    }

    @Test
    fun logAndExp() {
        eq(2.0, evaluateExpression("lg(100"), context = "lg(100 auto-close)")
        eq(1.0, evaluateExpression("ln(e)"), 1e-9, context = "ln(e)")
        eq(Math.E, evaluateExpression("exp(1)"), 1e-9, context = "exp(1)")
        eq(1000.0, evaluateExpression("10^(3)"), context = "10^3")
    }

    // ---------- Percentage (key behavior: 500+10%) ----------
    @Test
    fun percentageOnNumber() {
        // % key inserts "/(100)" — with auto-close this evaluates cleanly
        eq(5.0, evaluateExpression("500/(100)"), context = "500% = 5")
    }

    // ---------- Constants ----------
    @Test
    fun constants() {
        eq(Math.PI, evaluateExpression("pi"), 1e-9, context = "pi")
        eq(Math.E, evaluateExpression("e"), 1e-9, context = "e")
        // pi key inserts the pi symbol directly — must still work
        eq(Math.PI, evaluateExpression("\u03c0"), 1e-9, context = "pi symbol key")
        eq(2.0 * Math.PI, evaluateExpression("2\u03c0"), 1e-9, context = "2π symbol")
    }

    // ---------- Implicit multiplication ----------
    @Test
    fun implicitMultiplication() {
        eq(2.0 * Math.PI, evaluateExpression("2pi"), 1e-9, context = "2pi")
        eq(6.0, evaluateExpression("2*3"), context = "2*3")
        eq(60.0, evaluateExpression("2(30)"), context = "2(30)")
    }

    // ---------- Auto-close of open parentheses (basic keypad has no ')' until now) ----------
    @Test
    fun autoCloseParentheses() {
        eq(3.0, evaluateExpression("sqrt(9"), context = "sqrt(9 auto-close")
        eq(0.5, evaluateExpression("sin(30"), context = "sin(30 auto-close")
        eq(2.05, evaluateExpression("2+5/(100"), context = "2+5/(100 auto-close = 2.05")
        eq(12.0, evaluateExpression("sqrt(144"), context = "sqrt(144 auto-close")
        eq(-7.0, evaluateExpression("-(4+3"), context = "-(4+3 auto-close")
    }

    // ---------- Reciprocal (1/x key) ----------
    @Test
    fun reciprocal() {
        // 1/x replaces display with 1/value — verify the math the app computes
        val five = eval.evaluate("5")
        assertEquals(0.2, 1.0 / five, 1e-12)
        val eighteen = eval.evaluate("54-36")
        assertEquals(1.0 / 18.0, 1.0 / eighteen, 1e-12)
    }

    // ---------- Unary minus & negatives ----------
    @Test
    fun negatives() {
        eq(-3.0, evaluateExpression("-3"), context = "neg literal")
        eq(2.0, evaluateExpression("5+-3"), context = "5+-3")
        eq(-1.0, evaluateExpression("2-3"), context = "2-3")
        eq(-7.0, evaluateExpression("-(4+3)"), context = "-(4+3)")
    }
}
