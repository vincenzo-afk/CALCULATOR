package com.example.advancedcalculator

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // Hardcoded currency rates relative to USD
    private val currencyRates = mapOf(
        "USD" to 1.0,
        "EUR" to 0.91,
        "INR" to 82.0,
        "JPY" to 134.0,
        "GBP" to 0.78
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find UI elements by their IDs
        val inputCalc = findViewById<EditText>(R.id.inputCalc)
        val btnCalculate = findViewById<Button>(R.id.btnCalculate)
        val txtCalcResult = findViewById<TextView>(R.id.txtCalcResult)

        val spinnerConversionType = findViewById<Spinner>(R.id.spinnerConversionType)
        val spinnerFromUnit = findViewById<Spinner>(R.id.spinnerFromUnit)
        val spinnerToUnit = findViewById<Spinner>(R.id.spinnerToUnit)
        val inputValue = findViewById<EditText>(R.id.inputValue)
        val btnConvert = findViewById<Button>(R.id.btnConvert)
        val txtConversionResult = findViewById<TextView>(R.id.txtConversionResult)

        // Define conversion types and units
        val conversionTypes = listOf("Length", "Weight (Solid)", "Volume (Liquid)", "Currency")
        val lengthUnits = listOf("Meter", "Kilometer", "Centimeter", "Inch", "Foot")
        val weightUnits = listOf("Gram", "Kilogram", "Pound", "Ounce")
        val volumeUnits = listOf("Liter", "Milliliter", "Gallon", "Cup")
        val currencyUnits = currencyRates.keys.toList()

        // Set up spinner for conversion types
        val conversionTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conversionTypes)
        conversionTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerConversionType.adapter = conversionTypeAdapter

        // When user selects a conversion type, update the unit spinners accordingly
        spinnerConversionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val units = when (conversionTypes[position]) {
                    "Length" -> lengthUnits
                    "Weight (Solid)" -> weightUnits
                    "Volume (Liquid)" -> volumeUnits
                    "Currency" -> currencyUnits
                    else -> listOf()
                }
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_item, units)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerFromUnit.adapter = adapter
                spinnerToUnit.adapter = adapter
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Calculator button click listener
        btnCalculate.setOnClickListener {
            val expression = inputCalc.text.toString()
            val result = eval(expression)
            // Check if the result is NaN (Not a Number), which indicates an error
            if (result.isNaN()) {
                txtCalcResult.text = "Invalid Expression"
            } else {
                txtCalcResult.text = "Result: $result"
            }
        }

        // Convert button click listener
        btnConvert.setOnClickListener {
            // FIXED: Safely get the selected items. This prevents the app from crashing.
            val fromUnitItem = spinnerFromUnit.selectedItem
            val toUnitItem = spinnerToUnit.selectedItem
            val conversionTypeItem = spinnerConversionType.selectedItem

            // FIXED: Check if any spinner is unselected (null). If so, show a message and stop.
            if (fromUnitItem == null || toUnitItem == null || conversionTypeItem == null) {
                Toast.makeText(this, "Please ensure all units are selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Now it's safe to convert them to strings
            val fromUnit = fromUnitItem.toString()
            val toUnit = toUnitItem.toString()
            val conversionType = conversionTypeItem.toString()

            val valueStr = inputValue.text.toString()
            if (valueStr.isEmpty()) {
                Toast.makeText(this, "Enter a value to convert", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val value = valueStr.toDoubleOrNull()
            if (value == null) {
                Toast.makeText(this, "Invalid number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val convertedValue = when (conversionType) {
                "Length" -> convertLength(value, fromUnit, toUnit)
                "Weight (Solid)" -> convertWeight(value, fromUnit, toUnit)
                "Volume (Liquid)" -> convertVolume(value, fromUnit, toUnit)
                "Currency" -> convertCurrency(value, fromUnit, toUnit)
                else -> null
            }

            if (convertedValue == null) {
                txtConversionResult.text = "Conversion not supported"
            } else {
                txtConversionResult.text = "$value $fromUnit = $convertedValue $toUnit"
            }
        }
    }

    // Evaluate math expression using JavaScript engine
    private fun eval(expr: String): Double {
        val rhinoContext = org.mozilla.javascript.Context.enter()
        // FIXED: Use a try...finally block. This guarantees that Context.exit() is always
        // called, even if an error occurs. This prevents resource leaks and crashes.
        try {
            rhinoContext.optimizationLevel = -1 // Necessary for Android compatibility
            val scope: org.mozilla.javascript.Scriptable = rhinoContext.initStandardObjects()
            val result = rhinoContext.evaluateString(scope, expr, "JavaScript", 1, null)
            return (result as? Number)?.toDouble() ?: Double.NaN // Handle potential type issues
        } catch (e: Exception) {
            // Log the error for debugging purposes
            Log.e("EvalError", "Could not evaluate expression: $expr", e)
            return Double.NaN // Return NaN to indicate an error
        } finally {
            // This block will always execute, ensuring we don't leak resources.
            org.mozilla.javascript.Context.exit()
        }
    }

    // --- Conversion Helper Functions ---

    private fun convertLength(value: Double, from: String, to: String): Double? {
        val toMeters = when (from) {
            "Meter" -> 1.0
            "Kilometer" -> 1000.0
            "Centimeter" -> 0.01
            "Inch" -> 0.0254
            "Foot" -> 0.3048
            else -> return null
        }
        val fromMeters = when (to) {
            "Meter" -> 1.0
            "Kilometer" -> 1000.0
            "Centimeter" -> 0.01
            "Inch" -> 0.0254
            "Foot" -> 0.3048
            else -> return null
        }
        return value * toMeters / fromMeters
    }

    private fun convertWeight(value: Double, from: String, to: String): Double? {
        val toGrams = when (from) {
            "Gram" -> 1.0
            "Kilogram" -> 1000.0
            "Pound" -> 453.592
            "Ounce" -> 28.3495
            else -> return null
        }
        val fromGrams = when (to) {
            "Gram" -> 1.0
            "Kilogram" -> 1000.0
            "Pound" -> 453.592
            "Ounce" -> 28.3495
            else -> return null
        }
        return value * toGrams / fromGrams
    }

    private fun convertVolume(value: Double, from: String, to: String): Double? {
        val toLiters = when (from) {
            "Liter" -> 1.0
            "Milliliter" -> 0.001
            "Gallon" -> 3.78541
            "Cup" -> 0.24
            else -> return null
        }
        val fromLiters = when (to) {
            "Liter" -> 1.0
            "Milliliter" -> 0.001
            "Gallon" -> 3.78541
            "Cup" -> 0.24
            else -> return null
        }
        return value * toLiters / fromLiters
    }

    private fun convertCurrency(value: Double, from: String, to: String): Double? {
        val fromRate = currencyRates[from] ?: return null
        val toRate = currencyRates[to] ?: return null
        return value / fromRate * toRate
    }
}
