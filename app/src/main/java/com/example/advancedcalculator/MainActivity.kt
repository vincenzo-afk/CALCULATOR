package com.example.advancedcalculator

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private enum class Mode { CALCULATOR, SCIENTIFIC, HISTORY, CURRENCY }

    private lateinit var expressionEvaluator: ExpressionEvaluator

    // Memory register for m+, m-, mr, mc
    private var memory: Double = 0.0

    // 2nd toggle state (sin <-> asin, cos <-> acos, tan <-> atan, x² <-> x³, 10ˣ <-> log, eˣ <-> ln)
    private var secondFunction = false

    // History stored per day: map date string -> list of entries (newest first)
    private val historyByDate = LinkedHashMap<String, MutableList<String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        expressionEvaluator = ExpressionEvaluator()

        val displayInput = findViewById<EditText>(R.id.displayInput)
        val displayResult = findViewById<TextView>(R.id.displayResult)
        val memoryLabel = findViewById<TextView>(R.id.memoryLabel)
        val angleModeText = findViewById<TextView>(R.id.angleModeText)

        // Keep the display as a read-only keypad output (soft keyboard hidden)
        displayInput.keyListener = null
        displayInput.setText("")
        displayResult.text = "0"
        updateMemoryLabel(memoryLabel)
        loadHistory()

        // ---------------- Calculator keypad ----------------
        val lastAnswer = floatArrayOf(0.0f)

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
                        lastAnswer[0] = expressionEvaluator.evaluate(text).toFloat()
                        displayResult.text = formatResult(lastAnswer[0].toDouble())
                        addToHistory(text, formatResult(lastAnswer[0].toDouble()))
                        input.setText(formatResult(lastAnswer[0].toDouble()))
                    } catch (e: ExpressionEvaluator.EvalError) {
                        displayResult.text = e.message ?: "Error"
                        addToHistory(text, "Error")
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
                "Ans" -> formatResult(lastAnswer[0].toDouble())
                "DEG" -> {
                    val rad = angleModeText.text.toString() == "RAD"
                    expressionEvaluator = ExpressionEvaluator(
                        if (rad) ExpressionEvaluator.AngleUnit.RADIAN else ExpressionEvaluator.AngleUnit.DEGREE
                    )
                    angleModeText.text = if (rad) "RAD" else "DEG"
                    refreshDisplay(); return
                }
                "x²" -> "^(2)"
                "x³" -> "^(3)"
                "xʸ" -> "^"
                "10ˣ" -> "10^("
                "eˣ" -> "exp("
                "√(" -> "sqrt("
                "∛(" -> "cbrt("
                "lg(" -> "lg("
                "ln(" -> "ln("
                "sin(" -> if (secondFunction) "asin(" else "sin("
                "cos(" -> if (secondFunction) "acos(" else "cos("
                "tan(" -> if (secondFunction) "atan(" else "tan("
                "sinh(" -> "sinh("
                "cosh(" -> "cosh("
                "tanh(" -> "tanh("
                "1/x" -> "1/("
                "π" -> "pi"
                "EE" -> "e"
                "Rand" -> formatResult(Math.random())
                "%%" -> "/(100)"
                else -> token
            }
            input.setText(text + insert)
            refreshDisplay()
        }

        fun wire(ids: List<Int>, map: (String) -> String) {
            ids.forEach { id ->
                findViewById<Button>(id).setOnClickListener { onButtonClick(map("" )) }
            }
        }

        // Helper: shared mapping used by both keypads and the currency keypad
        fun calcMap(token: String) = onButtonClick(token)

        val calcButtons = listOf(
            R.id.btn0 to "0", R.id.btn1 to "1", R.id.btn2 to "2", R.id.btn3 to "3",
            R.id.btn4 to "4", R.id.btn5 to "5", R.id.btn6 to "6", R.id.btn7 to "7",
            R.id.btn8 to "8", R.id.btn9 to "9",
            R.id.btnDot to ".", R.id.btnAc to "AC", R.id.btnDel to "DEL",
            R.id.btnSwap to "±", R.id.btnAdd to "+", R.id.btnSub to "−",
            R.id.btnMul to "×", R.id.btnDiv to "÷",
            R.id.btnPercent to "%%", R.id.btnEq to "=",
            R.id.btnMemAdd to "M+", R.id.btnMemSub to "M-",
            R.id.btnMemRecall to "MR", R.id.btnMemClear to "MC"
        )
        calcButtons.forEach { (id, token) -> findViewById<Button>(id).setOnClickListener { calcMap(token) } }

        // Angle mode toggle (DEG/RAD)
        angleModeText.setOnClickListener {
            val rad = angleModeText.text.toString() == "RAD"
            expressionEvaluator = ExpressionEvaluator(
                if (rad) ExpressionEvaluator.AngleUnit.RADIAN else ExpressionEvaluator.AngleUnit.DEGREE
            )
            angleModeText.text = if (rad) "RAD" else "DEG"
            refreshDisplay()
        }

        // 2nd toggle
        val angleRad = { angleModeText.text.toString() == "RAD" }

        val btn2nd = findViewById<Button>(R.id.btn2nd)
        btn2nd.setOnClickListener {
            secondFunction = !secondFunction
            btn2nd.setTextColor(if (secondFunction) 0xFFFF4D4D.toInt() else 0xFFFFFFFF.toInt())
        }

        // Scientific keypad buttons
        val sciButtons = listOf(
            R.id.btnSciParenOpen to "(", R.id.btnSciParenClose to ")",
            R.id.btnSciTenPow to "10ˣ", R.id.btnSciPow to "xʸ",
            R.id.btnSciSq to "x²", R.id.btnSciCube to "x³",
            R.id.btnSciInv to "1/x", R.id.btnSciFact to "!",
            R.id.btnSciSqrt to "√(", R.id.btnSciCbrt to "∛(",
            R.id.btnSciLg to "lg(", R.id.btnSciLn to "ln(",
            R.id.btnSciSin to "sin(", R.id.btnSciCos to "cos(", R.id.btnSciTan to "tan(",
            R.id.btnSciSinh to "sinh(", R.id.btnSciCosh to "cosh(", R.id.btnSciTanh to "tanh(",
            R.id.btnSciExp to "eˣ", R.id.btnSciPi to "π", R.id.btnSciEe to "EE",
            R.id.btnSciRand to "Rand", R.id.btnSciPercent to "%%",
            R.id.btnSciAc to "AC", R.id.btnSciDel to "DEL", R.id.btnSciNeg to "±",
            R.id.btnSciAdd to "+", R.id.btnSciSub to "−", R.id.btnSciMul to "×",
            R.id.btnSciDiv to "÷", R.id.btnSciEq to "=",
            R.id.btnSciMc to "MC", R.id.btnSciMp to "M+",
            R.id.btnSciMm to "M-", R.id.btnSciMr to "MR"
        )
        sciButtons.forEach { (id, token) -> findViewById<Button>(id).setOnClickListener { calcMap(token) } }

        findViewById<Button>(R.id.btnSciRad).setOnClickListener {
            val rad = !angleRad()
            expressionEvaluator = ExpressionEvaluator(
                if (rad) ExpressionEvaluator.AngleUnit.RADIAN else ExpressionEvaluator.AngleUnit.DEGREE
            )
            angleModeText.text = if (rad) "RAD" else "DEG"
            refreshDisplay()
        }

        // Prevent soft keyboard focus
        displayInput.isFocusable = false
        displayInput.isCursorVisible = false

        // ---------------- Converter section ----------------
        val spinnerFromUnit = findViewById<Spinner>(R.id.spinnerFromUnit)
        val spinnerToUnit = findViewById<Spinner>(R.id.spinnerToUnit)
        val conversionResult = findViewById<TextView>(R.id.conversionResult)
        val ratesSourceNote = findViewById<TextView>(R.id.ratesSourceNote)

        val currencyUnits = CurrencyRepository.getCurrencies(this)
        val fromAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyUnits)
        fromAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        val toAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencyUnits)
        toAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFromUnit.adapter = fromAdapter
        spinnerToUnit.adapter = toAdapter

        // Defaults: USD -> INR if available
        val usdPos = currencyUnits.indexOf("USD")
        val inrPos = currencyUnits.indexOf("INR")
        if (usdPos >= 0) spinnerFromUnit.setSelection(usdPos)
        if (inrPos >= 0 && inrPos != usdPos) spinnerToUnit.setSelection(inrPos)

        fun convertCurrency() {
            val fromItem = spinnerFromUnit.selectedItem?.toString() ?: return
            val toItem = spinnerToUnit.selectedItem?.toString() ?: return
            val value = displayInput.text?.toString().orEmpty().toDoubleOrNull() ?: 0.0
            val converted = CurrencyRepository.convert(this, value, fromItem, toItem)
            if (converted == null) {
                conversionResult.text = "0"
            } else {
                conversionResult.text = formatResult(converted)
            }
            val dateStr = CurrencyRepository.lastDate(this)
            ratesSourceNote.text = "Data source: ECB reference rates${if (dateStr.isNotEmpty()) " · $dateStr" else ""}"
        }

        fun swapCurrencies() {
            val tmp = spinnerFromUnit.selectedItemPosition
            spinnerFromUnit.setSelection(spinnerToUnit.selectedItemPosition)
            spinnerToUnit.setSelection(tmp)
            convertCurrency()
        }

        fun convButtonClick(token: String) {
            val input = displayInput
            val text = input.text.toString()
            val insert = when (token) {
                "AC" -> { input.setText("0"); convertCurrency(); return }
                "DEL" -> {
                    val next = if (text.isNotEmpty()) text.dropLast(1) else ""
                    input.setText(next)
                    convertCurrency()
                    return
                }
                "=" -> { convertCurrency(); return }
                "00" -> {
                    if (text == "0") input.setText(text)
                    else input.setText(text + "00")
                    convertCurrency(); return
                }
                else -> token
            }
            val next = if (text == "0") insert else text + insert
            input.setText(next)
            convertCurrency()
        }

        spinnerFromUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) { convertCurrency() }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        spinnerToUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) { convertCurrency() }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        val fromCurrencyRow = findViewById<View>(R.id.fromCurrencyRow)
        val toCurrencyRow = findViewById<View>(R.id.toCurrencyRow)
        fromCurrencyRow.setOnClickListener { swapCurrencies() }
        toCurrencyRow.setOnClickListener { swapCurrencies() }

        val convButtons = listOf(
            R.id.btnConv0 to "0", R.id.btnConv1 to "1", R.id.btnConv2 to "2", R.id.btnConv3 to "3",
            R.id.btnConv4 to "4", R.id.btnConv5 to "5", R.id.btnConv6 to "6", R.id.btnConv7 to "7",
            R.id.btnConv8 to "8", R.id.btnConv9 to "9",
            R.id.btnConv00 to "00", R.id.btnConvDot to ".",
            R.id.btnConvAdd to "+", R.id.btnConvSub to "−",
            R.id.btnConvMul to "×", R.id.btnConvDiv to "÷",
            R.id.btnConvAc to "AC", R.id.btnConvDel to "DEL", R.id.btnConvEq to "="
        )
        convButtons.forEach { (id, token) -> findViewById<Button>(id).setOnClickListener { convButtonClick(token) } }

        // Initial conversion
        convertCurrency()

        // ---------------- Mode switching ----------------
        val iconHistory = findViewById<TextView>(R.id.iconHistory)
        val iconScientific = findViewById<TextView>(R.id.iconScientific)
        val iconCurrency = findViewById<TextView>(R.id.iconCurrency)
        val scientificKeypad = findViewById<View>(R.id.scientificKeypad)
        val basicKeypad = findViewById<View>(R.id.basicKeypad)
        val converterPanel = findViewById<View>(R.id.converterPanel)
        val converterKeypad = findViewById<View>(R.id.converterKeypad)
        val historyPanel = findViewById<View>(R.id.historyPanel)

        // Live conversion as the user types in currency mode
        displayInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) { convertCurrency() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        fun setMode(mode: Mode) {
            // Reset icon colors
            listOf(iconHistory, iconScientific, iconCurrency).forEach { it.setTextColor(0xFF8A8A8A.toInt()) }
            when (mode) {
                Mode.CALCULATOR -> {
                    iconHistory.setTextColor(0xFF8A8A8A.toInt())
                    scientificKeypad.visibility = View.GONE
                    basicKeypad.visibility = View.VISIBLE
                    converterPanel.visibility = View.GONE
                    converterKeypad.visibility = View.GONE
                    historyPanel.visibility = View.GONE
                    displayResult.setTextColor(0xFFFF4D4D.toInt())
                }
                Mode.SCIENTIFIC -> {
                    iconScientific.setTextColor(0xFFFF4D4D.toInt())
                    scientificKeypad.visibility = View.VISIBLE
                    basicKeypad.visibility = View.GONE
                    converterPanel.visibility = View.GONE
                    converterKeypad.visibility = View.GONE
                    historyPanel.visibility = View.GONE
                    displayResult.setTextColor(0xFFFF4D4D.toInt())
                }
                Mode.HISTORY -> {
                    iconHistory.setTextColor(0xFFFF4D4D.toInt())
                    scientificKeypad.visibility = View.GONE
                    basicKeypad.visibility = View.GONE
                    converterPanel.visibility = View.GONE
                    converterKeypad.visibility = View.GONE
                    historyPanel.visibility = View.VISIBLE
                    refreshHistoryPanel()
                }
                Mode.CURRENCY -> {
                    iconCurrency.setTextColor(0xFFFF4D4D.toInt())
                    scientificKeypad.visibility = View.GONE
                    basicKeypad.visibility = View.GONE
                    converterPanel.visibility = View.VISIBLE
                    converterKeypad.visibility = View.VISIBLE
                    historyPanel.visibility = View.GONE
                    displayResult.setTextColor(0xFF666666.toInt())
                }
            }
        }

        var currentMode = Mode.CALCULATOR

        iconScientific.setOnClickListener {
            if (currentMode == Mode.SCIENTIFIC) setMode(Mode.CALCULATOR) else setMode(Mode.SCIENTIFIC)
        }
        iconHistory.setOnClickListener {
            if (currentMode == Mode.HISTORY) setMode(Mode.CALCULATOR) else setMode(Mode.HISTORY)
        }
        iconCurrency.setOnClickListener {
            if (currentMode == Mode.CURRENCY) setMode(Mode.CALCULATOR) else setMode(Mode.CURRENCY)
        }

        setMode(Mode.CALCULATOR)

        findViewById<Button>(R.id.btnClearHistory).setOnClickListener {
            historyByDate.clear()
            saveHistory()
            refreshHistoryPanel()
        }

        // Hide keyboard when tapping outside inputs
        findViewById<View>(android.R.id.content).setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
        }
    }

    // ---------------- History ----------------
    private fun todayDate(): String = SimpleDateFormat("yyyy.MM.dd", Locale.US).format(Date())

    private fun loadHistory() {
        val prefs = getSharedPreferences("calc_history", Context.MODE_PRIVATE)
        val dates = prefs.getStringSet("dates", linkedSetOf()) ?: linkedSetOf()
        for (date in dates.sortedDescending()) {
            val entries = prefs.getStringSet("history_$date", linkedSetOf())
            if (!entries.isNullOrEmpty()) historyByDate[date] = entries.toMutableList()
        }
    }

    private fun saveHistory() {
        val prefs = getSharedPreferences("calc_history", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        val editor = prefs.edit()
        editor.putStringSet("dates", historyByDate.keys.toHashSet())
        historyByDate.forEach { (date, entries) ->
            editor.putStringSet("history_$date", entries.toHashSet())
        }
        editor.apply()
    }

    private fun addToHistory(expression: String, result: String) {
        if (expression.isEmpty()) return
        val date = todayDate()
        historyByDate.getOrPut(date) { mutableListOf() }.add(0, "$expression=$result")
        saveHistory()
    }

    private fun refreshHistoryPanel() {
        val list = findViewById<LinearLayout>(R.id.historyList)
        list.removeAllViews()
        for ((date, entries) in historyByDate.toSortedMap(compareByDescending { it })) {
            val dateLabel = TextView(this).apply {
                text = date
                setTextColor(0xFFFF4D4D.toInt())
                textSize = 13f
                gravity = android.view.Gravity.END
                setPadding(0, 16, 0, 10)
            }
            list.addView(dateLabel)
            val buf = StringBuilder()
            for (entry in entries) {
                buf.append(entry).append("\n\n")
            }
            val entryText = TextView(this).apply {
                text = buf.toString().trimEnd()
                setTextColor(0xFF999999.toInt())
                textSize = 16f
                gravity = android.view.Gravity.END
            }
            list.addView(entryText)
        }
    }

    private fun updateMemoryLabel(label: TextView) {
        label.visibility = if (memory != 0.0) View.VISIBLE else View.GONE
        if (memory != 0.0) label.text = "M = ${formatResult(memory)}"
    }
}
