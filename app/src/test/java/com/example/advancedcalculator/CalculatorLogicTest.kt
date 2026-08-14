package com.example.advancedcalculator

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.*

class CalculatorLogicTest {

    private val eval = ExpressionEvaluator()
    private val evalRad = ExpressionEvaluator(ExpressionEvaluator.AngleUnit.RADIAN)

    private fun eq(expr: String, expected: Double, tolerance: Double = 1e-9) {
        assertEquals(expected, eval.evaluate(expr), tolerance)
    }

    @Test
    fun basicArithmetic() {
        eq("2+3", 5.0)
        eq("2+3*4", 14.0)
        eq("(2+3)*4", 20.0)
        eq("10-4", 6.0)
        eq("12/3", 4.0)
        eq("7%3", 1.0)
    }

    @Test
    fun implicitMultiplication() {
        eq("2(3+1)", 8.0)
        eq("2pi", 2 * PI, 1e-9)
        eq("3(4)", 12.0)
    }

    @Test
    fun powerAndFactorial() {
        eq("2^10", 1024.0)
        eq("2^3^2", 512.0) // right-associative: 2^(3^2) = 512
        eq("5!", 120.0)
        eq("0!", 1.0)
    }

    @Test
    fun constants() {
        eq("pi", PI)
        eq("e", E)
        eq("tau", 2 * PI)
    }

    @Test
    fun functions() {
        eq("sqrt(16)", 4.0)
        eq("log(100)", 2.0)
        eq("ln(e)", 1.0)
        eq("abs(-7)", 7.0)
        eq("round(2.6)", 3.0)
        eq("floor(2.6)", 2.0)
        eq("ceil(2.1)", 3.0)
    }

    @Test
    fun trigDegrees() {
        assertEquals(0.5, eval.evaluate("sin(30)"), 1e-9)
        assertEquals(1.0, eval.evaluate("cos(0)"), 1e-9)
        assertEquals(1.0, eval.evaluate("tan(45)"), 1e-9)
        assertEquals(30.0, eval.evaluate("asin(0.5)"), 1e-9)
    }

    @Test
    fun trigRadians() {
        assertEquals(1.0, evalRad.evaluate("sin(pi/2)"), 1e-9)
        assertEquals(0.0, evalRad.evaluate("cos(pi/2)"), 1e-6)
    }

    @Test(expected = ExpressionEvaluator.EvalError.DivisionByZero::class)
    fun divisionByZero() {
        eval.evaluate("1/0")
    }

    @Test(expected = ExpressionEvaluator.EvalError.DomainError::class)
    fun sqrtNegative() {
        eval.evaluate("sqrt(-1)")
    }

    @Test(expected = ExpressionEvaluator.EvalError.InvalidSyntax::class)
    fun invalidSyntax() {
        eval.evaluate("2++")
    }

    @Test
    fun injectionAttemptBlocked() {
        // Rhino eval injection attempts must now be simple syntax errors
        assertThrows(ExpressionEvaluator.EvalError::class.java) {
            eval.evaluate("Runtime.getRuntime().exec('x')")
        }
        assertThrows(ExpressionEvaluator.EvalError::class.java) {
            eval.evaluate("java.lang.System.exit(0)")
        }
    }

    @Test
    fun lengthConversions() {
        // 1 mile = 1.609344 km
        assertEquals(1.609344, convertLen(1.0, "Mile", "Kilometer"), 1e-6)
        // 1 foot = 12 inches
        assertEquals(12.0, convertLen(1.0, "Foot", "Inch"), 1e-9)
    }

    @Test
    fun weightConversions() {
        assertEquals(2.2046226, convertWeight(1.0, "Kilogram", "Pound"), 1e-5)
        assertEquals(1000.0, convertWeight(1.0, "Kilogram", "Gram"), 1e-9)
    }

    @Test
    fun volumeConversions() {
        assertEquals(3.785411784, convertVolume(1.0, "Gallon (US)", "Liter"), 1e-6)
        assertEquals(16.0, convertVolume(1.0, "Gallon (US)", "Cup (US)"), 1e-6)
    }

    @Test
    fun temperatureConversions() {
        assertEquals(212.0, convertTemp(100.0, "Celsius", "Fahrenheit")!!, 1e-9)
        assertEquals(373.15, convertTemp(100.0, "Celsius", "Kelvin")!!, 1e-9)
        assertEquals(32.0, convertTemp(0.0, "Celsius", "Fahrenheit")!!, 1e-9)
        // below absolute zero must be rejected
        assertNull(convertTemp(-500.0, "Celsius", "Fahrenheit"))
    }

    @Test
    fun resultFormatting() {
        assertEquals("0.3", formatResult(0.1 + 0.2))
        assertEquals("NaN", formatResult(Double.NaN))
        assertEquals("Error", formatResult(Double.POSITIVE_INFINITY))
    }

    // mirror helpers (same logic as MainActivity) for JVM unit tests
    private fun convertLen(v: Double, f: String, t: String): Double {
        val m = mapOf(
            "Millimeter" to 0.001, "Centimeter" to 0.01, "Meter" to 1.0,
            "Kilometer" to 1000.0, "Inch" to 0.0254, "Foot" to 0.3048,
            "Yard" to 0.9144, "Mile" to 1609.344, "Nautical Mile" to 1852.0
        )
        return v * m[f]!! / m[t]!!
    }

    private fun convertWeight(v: Double, f: String, t: String): Double {
        val g = mapOf(
            "Milligram" to 0.001, "Gram" to 1.0, "Kilogram" to 1000.0,
            "Ounce" to 28.349523125, "Pound" to 453.59237,
            "Stone" to 6350.29318, "Metric Ton" to 1_000_000.0
        )
        return v * g[f]!! / g[t]!!
    }

    private fun convertVolume(v: Double, f: String, t: String): Double {
        val l = mapOf(
            "Milliliter" to 0.001, "Liter" to 1.0, "Cup (US)" to 0.2365882365,
            "Pint (US)" to 0.473176473, "Quart (US)" to 0.946352946,
            "Gallon (US)" to 3.785411784, "Fluid Ounce (US)" to 0.0295735295625,
            "Tablespoon (US)" to 0.01478676478125, "Teaspoon (US)" to 0.00492892159375
        )
        return v * l[f]!! / l[t]!!
    }

    private fun convertTemp(v: Double, f: String, t: String): Double? {
        val k = when (f) {
            "Celsius" -> v + 273.15
            "Fahrenheit" -> (v - 32) * 5 / 9 + 273.15
            "Kelvin" -> v
            else -> return null
        }
        if (k < 0) return null
        return when (t) {
            "Celsius" -> k - 273.15
            "Fahrenheit" -> (k - 273.15) * 9 / 5 + 32
            "Kelvin" -> k
            else -> null
        }
    }
}
