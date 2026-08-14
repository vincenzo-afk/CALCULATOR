package com.example.advancedcalculator

/**
 * Static unit conversions (no network needed).
 * Each category stores factors relative to a single base unit
 * (e.g. meter for length). Conversion: value / fromFactor * toFactor.
 */
object UnitConverter {

    // value in base unit per one unit
    val LENGTH: LinkedHashMap<String, Double> = linkedMapOf(
        "Millimeter (mm)" to 0.001,
        "Centimeter (cm)" to 0.01,
        "Meter (m)" to 1.0,
        "Kilometer (km)" to 1000.0,
        "Inch (in)" to 0.0254,
        "Foot (ft)" to 0.3048,
        "Yard (yd)" to 0.9144,
        "Mile (mi)" to 1609.344,
        "Nautical mile" to 1852.0,
        "Micrometer (μm)" to 0.000001,
        "Light year" to 9.4607e+15,
        "Mile (nautical)" to 1852.0
    )

    val WEIGHT: LinkedHashMap<String, Double> = linkedMapOf(
        "Milligram (mg)" to 0.000001,
        "Gram (g)" to 0.001,
        "Kilogram (kg)" to 1.0,
        "Metric ton (t)" to 1000.0,
        "Ounce (oz)" to 0.028349523,
        "Pound (lb)" to 0.45359237,
        "Stone (st)" to 6.350293,
        "Carat (ct)" to 0.0002
    )

    val VOLUME: LinkedHashMap<String, Double> = linkedMapOf(
        "Milliliter (mL)" to 0.000001,
        "Liter (L)" to 0.001,
        "Cubic centimeter (cm³)" to 0.000001,
        "Cubic meter (m³)" to 1.0,
        "Gallon (US)" to 0.0037854118,
        "Quart (US)" to 0.0009463529,
        "Pint (US)" to 0.0004731765,
        "Cup (US)" to 0.0002365882,
        "Fluid ounce (US)" to 0.0000295735,
        "Tablespoon" to 0.0000147868,
        "Teaspoon" to 0.0000049290
    )

    val AREA: LinkedHashMap<String, Double> = linkedMapOf(
        "Square millimeter (mm²)" to 0.000001,
        "Square centimeter (cm²)" to 0.0001,
        "Square meter (m²)" to 1.0,
        "Hectare (ha)" to 10000.0,
        "Square kilometer (km²)" to 1000000.0,
        "Square inch (in²)" to 0.00064516,
        "Square foot (ft²)" to 0.09290304,
        "Square yard (yd²)" to 0.83612736,
        "Acre (ac)" to 4046.8564224,
        "Square mile (mi²)" to 2589988.1103
    )

    val DATA: LinkedHashMap<String, Double> = linkedMapOf(
        "Bit (b)" to 0.125,
        "Byte (B)" to 1.0,
        "Kilobyte (KB)" to 1000.0,
        "Megabyte (MB)" to 1000000.0,
        "Gigabyte (GB)" to 1e9,
        "Terabyte (TB)" to 1e12,
        "Petabyte (PB)" to 1e15,
        "Kibibyte (KiB)" to 1024.0,
        "Mebibyte (MiB)" to 1048576.0,
        "Gibibyte (GiB)" to 1073741824.0
    )

    val TIME: LinkedHashMap<String, Double> = linkedMapOf(
        "Millisecond (ms)" to 0.001,
        "Second (s)" to 1.0,
        "Minute (min)" to 60.0,
        "Hour (h)" to 3600.0,
        "Day (d)" to 86400.0,
        "Week (wk)" to 604800.0,
        "Month (avg, 30.44 d)" to 2629746.0,
        "Year (avg, 365.25 d)" to 31557600.0
    )

    // Temperature is special: not linear from a base.
    val TEMPERATURE = listOf("Celsius (°C)", "Fahrenheit (°F)", "Kelvin (K)")

    fun categories(): List<String> =
        listOf("Currency", "Length", "Weight", "Volume", "Area", "Data", "Time", "Temperature")

    fun unitsOf(category: String): List<String> = when (category) {
        "Length" -> LENGTH.keys.toList()
        "Weight" -> WEIGHT.keys.toList()
        "Volume" -> VOLUME.keys.toList()
        "Area" -> AREA.keys.toList()
        "Data" -> DATA.keys.toList()
        "Time" -> TIME.keys.toList()
        "Temperature" -> TEMPERATURE
        else -> emptyList()
    }

    fun convert(category: String, value: Double, from: String, to: String): Double? {
        if (from == to) return value
        return when (category) {
            "Length" -> LENGTH[from]?.let { f -> LENGTH[to]?.let { t -> value * f / t } }
            "Weight" -> WEIGHT[from]?.let { f -> WEIGHT[to]?.let { t -> value * f / t } }
            "Volume" -> VOLUME[from]?.let { f -> VOLUME[to]?.let { t -> value * f / t } }
            "Area" -> AREA[from]?.let { f -> AREA[to]?.let { t -> value * f / t } }
            "Data" -> DATA[from]?.let { f -> DATA[to]?.let { t -> value * f / t } }
            "Time" -> TIME[from]?.let { f -> TIME[to]?.let { t -> value * f / t } }
            "Temperature" -> if (from in TEMPERATURE && to in TEMPERATURE) convertTemperature(value, from, to) else null
            else -> null
        }
    }

    private fun toCelsius(value: Double, unit: String): Double = when (unit) {
        "Celsius (°C)" -> value
        "Fahrenheit (°F)" -> (value - 32.0) * 5.0 / 9.0
        "Kelvin (K)" -> value - 273.15
        else -> value
    }

    private fun fromCelsius(celsius: Double, unit: String): Double = when (unit) {
        "Celsius (°C)" -> celsius
        "Fahrenheit (°F)" -> celsius * 9.0 / 5.0 + 32.0
        "Kelvin (K)" -> celsius + 273.15
        else -> celsius
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double =
        fromCelsius(toCelsius(value, from), to)
}
