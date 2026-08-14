package com.example.advancedcalculator

import org.junit.Assert.*
import org.junit.Test

class UnitConverterTest {

    private fun eq(expected: Double, actual: Double?, tolerance: Double = 1e-9) {
        assertNotNull("conversion returned null", actual)
        assertEquals(expected, actual!!, tolerance)
    }

    @Test
    fun lengthConversions() {
        eq(1609.344, UnitConverter.convert("Length", 1.0, "Mile (mi)", "Meter (m)"))
        eq(12.0, UnitConverter.convert("Length", 1.0, "Foot (ft)", "Inch (in)"))
        eq(1.0, UnitConverter.convert("Length", 1000.0, "Millimeter (mm)", "Meter (m)"))
        eq(2.54, UnitConverter.convert("Length", 1.0, "Inch (in)", "Centimeter (cm)"))
    }

    @Test
    fun weightConversions() {
        eq(453.59237, UnitConverter.convert("Weight", 1.0, "Pound (lb)", "Gram (g)"))
        eq(16.0, UnitConverter.convert("Weight", 1.0, "Pound (lb)", "Ounce (oz)"), 1e-4)
        eq(1.0, UnitConverter.convert("Weight", 1000.0, "Gram (g)", "Kilogram (kg)"))
    }

    @Test
    fun volumeConversions() {
        eq(3785.4118, UnitConverter.convert("Volume", 1.0, "Gallon (US)", "Milliliter (mL)"), 1e-3)
        eq(16.0, UnitConverter.convert("Volume", 1.0, "Gallon (US)", "Cup (US)"), 1e-3)
        eq(1.0, UnitConverter.convert("Volume", 1000.0, "Milliliter (mL)", "Liter (L)"))
    }

    @Test
    fun areaConversions() {
        eq(0.09290304, UnitConverter.convert("Area", 1.0, "Square foot (ft²)", "Square meter (m²)"))
        eq(43560.0, UnitConverter.convert("Area", 1.0, "Acre (ac)", "Square foot (ft²)"), 1e-2)
    }

    @Test
    fun dataConversions() {
        eq(1000.0, UnitConverter.convert("Data", 1.0, "Kilobyte (KB)", "Byte (B)"))
        eq(1024.0, UnitConverter.convert("Data", 1.0, "Kibibyte (KiB)", "Byte (B)"))
        eq(8.0, UnitConverter.convert("Data", 1.0, "Byte (B)", "Bit (b)"))
    }

    @Test
    fun timeConversions() {
        eq(3600.0, UnitConverter.convert("Time", 1.0, "Hour (h)", "Second (s)"))
        eq(24.0, UnitConverter.convert("Time", 1.0, "Day (d)", "Hour (h)"))
    }

    @Test
    fun temperatureConversions() {
        eq(32.0, UnitConverter.convert("Temperature", 0.0, "Celsius (°C)", "Fahrenheit (°F)"))
        eq(212.0, UnitConverter.convert("Temperature", 100.0, "Celsius (°C)", "Fahrenheit (°F)"))
        eq(373.15, UnitConverter.convert("Temperature", 100.0, "Celsius (°C)", "Kelvin (K)"))
        eq(273.15, UnitConverter.convert("Temperature", 32.0, "Fahrenheit (°F)", "Kelvin (K)"))
        eq(0.0, UnitConverter.convert("Temperature", 32.0, "Fahrenheit (°F)", "Celsius (°C)"))
    }

    @Test
    fun sameUnitReturnsValue() {
        eq(5.5, UnitConverter.convert("Length", 5.5, "Meter (m)", "Meter (m)"))
        eq(7.0, UnitConverter.convert("Temperature", 7.0, "Kelvin (K)", "Kelvin (K)"))
    }

    @Test
    fun unknownUnitsReturnNull() {
        assertNull(UnitConverter.convert("Length", 1.0, "Meter (m)", "Banana"))
        assertNull(UnitConverter.convert("Unknown", 1.0, "A", "B"))
    }

    @Test
    fun categoriesAndUnitsNotEmpty() {
        for (cat in UnitConverter.categories().filter { it != "Currency" }) {
            assertTrue("$cat has no units", UnitConverter.unitsOf(cat).isNotEmpty())
        }
    }
}
