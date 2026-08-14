package com.example.advancedcalculator

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var expressionEvaluator: ExpressionEvaluator

    // Memory register for M+, M-, MR, MC
    private var memory: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        expressionEvaluator = ExpressionEvaluator()

        val displayInput = findViewById<EditText>(R.id.displayInput)
        val displayResult = findViewById<TextView>(R.id.displayResult)
        val memoryLabel = findViewById<TextView>(R.id.memoryLabel)
        val angleModeBtn = findViewById<ToggleButton>(R.id.btnAngleMode)

        // Keep the display as a read-only keypad output (soft keyboard hidden)
        displayInput.keyListener = null
        displayInput.setText("")
        displayResult.text = "0"
        updateMemoryLabel(memoryLabel)

        // ---------------- Calculator keypad ----------------
        val buttons: Map<Int, String> = mapOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
            R.id.btn8 to "8", R.id.btn9 to "9",
            R.id.btnDot to ".", R.id.btnOpenParen to "(", R.id.btnCloseParen to ")",
            R.id.btnAdd to "+", R.id.btnSub to "−", R.id.btnMul to "×", R.id.btnDiv to "÷",
            R.id.btnMod to "%", R.id.btnPow to "^", R.id.btnPi to "π", R.id.btnE to "e",
            R.id.btnSqrt to "√(", R.id.btnSq to "²",
            R.id.btnSin to "sin(", R.id.btnCos to "cos(", R.id.btnTan to "tan(",
            R.id.btnLog to "log(", R.id.btnLn to "ln(", R.id.btnFact to "!",
            R.id.btnPercent to "%%",      // display "x%" as (x/100)
            R.id.btnAns to "Ans", R.id.btnSwap to "±",
            R.id.btnAc to "AC", R.id.btnDel to "DEL", R.id.btnEq to "=",
            R.id.btnMemAdd to "M+", R.id.btnMemSub to "M-",
            R.id.btnMemRecall to "MR", R.id.btnMemClear to "MC"
        )

        var lastAnswer: Double = 0.0

        fun refreshDisplay() {
            val expr = displayInput.text?.toString().orEmpty()
            displayResult.text = if (expr.isEmpty()) "0" else try {
                val preview = expressionEvaluator.evaluate(expr)
                formatResult(preview)
            } catch (_: Exception) {
                ""
            }
        }

        fun onButtonClick(token: String) {
            val input = displayInput
            val text = input.text.toString()
            val insert = when (token) {
                "AC" -> { input.setText(""); refreshDisplay(); return }
                "DEL" -> {
                    if (text.isNotEmpty()) input.setText(text.dropLast(1))
                    refreshDisplay()
                    return
                }
                "±" -> {
                    val neg = if (text.startsWith("−")) text.drop(1) else "−$text"
                    input.setText(neg); refreshDisplay(); return
                }
                "=" -> {
                    try {
                        lastAnswer = expressionEvaluator.evaluate(text)
                        displayResult.text = formatResult(lastAnswer)
                        input.setText(formatResult(lastAnswer))
                    } catch (e: ExpressionEvaluator.EvalError) {
                        displayResult.text = e.message ?: "Error"
                    }
                    return
                }
                "M+" -> {
                    try {
                        memory += expressionEvaluator.evaluate(text)
                    } catch (_: Exception) { /* ignore bad expression */ }
                    updateMemoryLabel(memoryLabel); return
                }
                "M-" -> {
                    try {
                        memory -= expressionEvaluator.evaluate(text)
                    } catch (_: Exception) { /* ignore bad expression */ }
                    updateMemoryLabel(memoryLabel); return
                }
                "MR" -> {
                    input.setText(text + formatResult(memory)); refreshDisplay(); return
                }
                "MC" -> {
                    memory = 0.0; updateMemoryLabel(memoryLabel); return
                }
                "DEG" -> {
                    val rad = !angleModeBtn.isChecked
                    expressionEvaluator = ExpressionEvaluator(
                        if (rad) ExpressionEvaluator.AngleUnit.RADIAN else ExpressionEvaluator.AngleUnit.DEGREE
                    )
                    refreshDisplay(); return
                }
                "Ans" -> formatResult(lastAnswer)
                "x²" -> "^(2)"
                "%%" -> "/(100)"
                else -> token
            }
            input.setText(text + insert)
            refreshDisplay()
        }

        buttons.forEach { (id, token) ->
            findViewById<Button>(id).setOnClickListener { onButtonClick(token) }
        }

        // Swap angles toggle (DEG/RAD)
        angleModeBtn.setOnCheckedChangeListener { _, isChecked ->
            // isChecked == true  => RADIAN, false => DEGREE
            expressionEvaluator = ExpressionEvaluator(
                if (isChecked) ExpressionEvaluator.AngleUnit.RADIAN else ExpressionEvaluator.AngleUnit.DEGREE
            )
            refreshDisplay()
        }

        // Prevent soft keyboard focus
        displayInput.isFocusable = false
        displayInput.isCursorVisible = false

        // ---------------- Converter section ----------------
        val spinnerConversionType = findViewById<Spinner>(R.id.spinnerConversionType)
        val spinnerFromUnit = findViewById<Spinner>(R.id.spinnerFromUnit)
        val spinnerToUnit = findViewById<Spinner>(R.id.spinnerToUnit)
        val inputValue = findViewById<EditText>(R.id.inputValue)
        val btnConvert = findViewById<Button>(R.id.btnConvert)
        val txtConversionResult = findViewById<TextView>(R.id.txtConversionResult)

        val conversionTypes = listOf("Length", "Weight", "Volume", "Temperature", "Currency")
        val lengthUnits = listOf("Millimeter", "Centimeter", "Meter", "Kilometer", "Inch", "Foot", "Yard", "Mile", "Nautical Mile")
        val weightUnits = listOf("Milligram", "Gram", "Kilogram", "Ounce", "Pound", "Stone", "Metric Ton")
        val volumeUnits = listOf("Milliliter", "Liter", "Cup (US)", "Pint (US)", "Quart (US)", "Gallon (US)", "Fluid Ounce (US)", "Tablespoon (US)", "Teaspoon (US)")
        val currencyUnits = CurrencyRepository.getCurrencies(this)

        val conversionTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, conversionTypes)
        conversionTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerConversionType.adapter = conversionTypeAdapter

        spinnerConversionType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val units = when (conversionTypes[position]) {
                    "Length" -> lengthUnits
                    "Weight" -> weightUnits
                    "Volume" -> volumeUnits
                    "Temperature" -> listOf("Celsius", "Fahrenheit", "Kelvin")
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
        // Trigger initial population
        spinnerConversionType.setSelection(0, false)
        spinnerConversionType.performItemClick(
            spinnerConversionType.getChildAt(0), 0,
            spinnerConversionType.adapter.getItemId(0)
        )

        btnConvert.setOnClickListener {
            val fromUnitItem = spinnerFromUnit.selectedItem
            val toUnitItem = spinnerToUnit.selectedItem
            val conversionTypeItem = spinnerConversionType.selectedItem

            if (fromUnitItem == null || toUnitItem == null || conversionTypeItem == null) {
                Toast.makeText(this, "Please ensure all units are selected", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

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
                "Weight" -> convertWeight(value, fromUnit, toUnit)
                "Volume" -> convertVolume(value, fromUnit, toUnit)
                "Temperature" -> convertTemperature(value, fromUnit, toUnit)
                "Currency" -> CurrencyRepository.convert(this, value, fromUnit, toUnit)
                else -> null
            }

            if (convertedValue == null) {
                txtConversionResult.text = "Conversion not supported"
            } else {
                txtConversionResult.text = "$value $fromUnit = ${formatResult(convertedValue)} $toUnit"
            }
        }

        // Hide keyboard when tapping outside inputs
        findViewById<View>(android.R.id.content).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    private fun updateMemoryLabel(label: TextView) {
        label.visibility = if (memory != 0.0) View.VISIBLE else View.GONE
        if (memory != 0.0) label.text = "M = ${formatResult(memory)}"
    }

    // ---------------- Conversion helpers ----------------
    // All length factors convert to meters; weight to grams; volume to liters.

    private fun convertLength(value: Double, from: String, to: String): Double? {
        val m = mapOf(
            "Millimeter" to 0.001, "Centimeter" to 0.01, "Meter" to 1.0,
            "Kilometer" to 1000.0, "Inch" to 0.0254, "Foot" to 0.3048,
            "Yard" to 0.9144, "Mile" to 1609.344, "Nautical Mile" to 1852.0
        )
        return value * (m[from] ?: return null) / (m[to] ?: return null)
    }

    private fun convertWeight(value: Double, from: String, to: String): Double? {
        val g = mapOf(
            "Milligram" to 0.001, "Gram" to 1.0, "Kilogram" to 1000.0,
            "Ounce" to 28.349523125, "Pound" to 453.59237,
            "Stone" to 6350.29318, "Metric Ton" to 1_000_000.0
        )
        return value * (g[from] ?: return null) / (g[to] ?: return null)
    }

    private fun convertVolume(value: Double, from: String, to: String): Double? {
        val l = mapOf(
            "Milliliter" to 0.001, "Liter" to 1.0, "Cup (US)" to 0.2365882365,
            "Pint (US)" to 0.473176473, "Quart (US)" to 0.946352946,
            "Gallon (US)" to 3.785411784, "Fluid Ounce (US)" to 0.0295735295625,
            "Tablespoon (US)" to 0.01478676478125, "Teaspoon (US)" to 0.00492892159375
        )
        return value * (l[from] ?: return null) / (l[to] ?: return null)
    }

    private fun convertTemperature(value: Double, from: String, to: String): Double? {
        // Convert everything to Kelvin first (absolute scale handles negatives correctly)
        val kelvin = when (from) {
            "Celsius" -> value + 273.15
            "Fahrenheit" -> (value - 32) * 5 / 9 + 273.15
            "Kelvin" -> value
            else -> return null
        }
        if (kelvin < 0) return null // below absolute zero
        return when (to) {
            "Celsius" -> kelvin - 273.15
            "Fahrenheit" -> (kelvin - 273.15) * 9 / 5 + 32
            "Kelvin" -> kelvin
            else -> null
        }
    }
}
