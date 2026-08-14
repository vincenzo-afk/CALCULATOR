package com.example.advancedcalculator

import android.content.Context
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Real-time currency rates fetched from the Frankfurter API
 * (https://frankfurter.app) which publishes European Central Bank reference
 * rates for ~170 currencies. Free, no API key required.
 *
 * Rates are cached for the remainder of the day to avoid excessive requests,
 * and the cached file persists across app restarts so conversions still work
 * offline with the last known rates.
 */
object CurrencyRepository {

    private const val BASE_URL = "https://api.frankfurter.app"
    private const val CACHE_KEY_DATE = "rates_date"
    private const val CACHE_KEY_RATES = "rates_json"
    private const val PREFS = "currency_prefs"

    private val cachedRates = ConcurrentHashMap<String, Double>()

    fun getRates(context: Context): Map<String, Double> {
        val today = java.time.LocalDate.now().toString()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (cachedRates.isNotEmpty()) return cachedRates

        val cachedDate = prefs.getString(CACHE_KEY_DATE, "")
        val cachedJson = prefs.getString(CACHE_KEY_RATES, "")
        if (cachedDate == today && !cachedJson.isNullOrEmpty()) {
            loadFromJson(cachedJson)
            return cachedRates
        }

        // Fetch fresh rates
        try {
            val url = URL("$BASE_URL/latest?from=USD")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val body = reader.readText()
                reader.close()
                prefs.edit()
                    .putString(CACHE_KEY_DATE, today)
                    .putString(CACHE_KEY_RATES, body)
                    .apply()
                loadFromJson(body)
            }
        } catch (_: Exception) {
            // network error: prefer previously cached rates, else bundled fallback
            if (cachedJson.isNullOrEmpty()) {
                loadFromJson(fallbackJson)
            }
        }
        return cachedRates
    }

    fun lastDate(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CACHE_KEY_DATE, "") ?: ""

    fun getCurrencies(context: Context): List<String> =
        getRates(context).keys.sorted()

    fun convert(context: Context, value: Double, from: String, to: String): Double? {
        val rates = getRates(context)
        val fromRate = rates[from] ?: return null
        val toRate = rates[to] ?: return null
        // rates are "units per 1 USD": USD -> X means multiply by X; X -> USD divide by X
        return value / fromRate * toRate
    }

    private fun loadFromJson(json: String) {
        try {
            val root = JSONObject(json)
            val ratesObj = root.optJSONObject("rates")
            if (ratesObj != null) {
                cachedRates.clear()
                cachedRates["USD"] = 1.0
                val keys = ratesObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    cachedRates[key] = ratesObj.optDouble(key, 0.0)
                }
            }
        } catch (_: Exception) {
            // keep whatever was cached before
        }
    }

    private val fallbackJson = """
        {"amount":1.0,"base":"USD","date":"2026-01-01","rates":{
        "EUR":0.954,"GBP":0.785,"INR":84.5,"JPY":149.5,"CNY":7.18,"AUD":1.52,
        "CAD":1.37,"CHF":0.884,"HKD":7.78,"SGD":1.34,"SEK":10.5,"NOK":10.7,
        "NZD":1.66,"MXN":19.5,"BRL":5.45,"ZAR":18.2,"KRW":1340.0,"TRY":34.5,
        "RUB":92.0,"THB":34.8,"MYR":4.45,"IDR":15900.0,"PHP":56.5,"PLN":4.02,
        "DKK":7.13,"CZK":23.2,"HUF":370.0,"RON":4.75,"ILS":3.65,"AED":3.6725,
        "SAR":3.75,"EGP":48.5,"NGN":1560.0,"KES":129.0,"GHS":14.8,"PKR":279.0,
        "BDT":119.0,"LKR":305.0,"VND":25300.0,"ARS":985.0,"CLP":940.0,"COP":4100.0,
        "PEN":3.72,"UAH":41.2}}
    """.trimIndent()
}
