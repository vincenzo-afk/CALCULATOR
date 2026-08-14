# CALCULATOR

A full-featured advanced calculator and unit converter for Android, written in Kotlin.

## Features

### Scientific Calculator Keypad

A real calculator keypad with all standard keys and symbols, including digits `0`–`9`, decimal point, basic operators `+ − × ÷`, equals, and the full scientific toolset:

| Group | Keys |
| --- | --- |
| Basic | `0–9`, `.` , `=`, `AC`, `DEL`, `±` |
| Operators | `+`, `−`, `×`, `÷`, `%` (modulus), `^` (power) |
| Scientific | `sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `log` (base 10), `ln`, `√`, `x²`, `x!` (factorial), `π`, `e`, `|x|` |
| Parentheses | `(`, `)` with implicit multiplication (`2(3+1)`, `2π`) |
| Utilities | `Ans` (last answer), memory (`M+`, `M−`, `MR`, `MC`), DEG/RAD toggle, calculation history |

Trigonometric functions use degrees by default (like the Google Calculator app) and can be switched to radians with the DEG/RAD toggle.

### Real-Time Currency Conversion

Live exchange rates for **160+ currencies** (EUR, GBP, INR, JPY, CNY, USD, and many more) fetched from the [Frankfurter API](https://frankfurter.app) (European Central Bank reference rates). Rates are cached for the whole day so conversions are instant, and the last known rates are bundled as a fallback so conversion still works offline.

### Unit Converter

Additional conversion categories beyond currency: **length**, **weight**, **volume**, and **temperature** (Celsius, Fahrenheit, Kelvin), all with proper domain validation.

## Bugs Fixed in This Version

| # | Bug | Fix |
| --- | --- | --- |
| 1 | **Critical security vulnerability**: the old app passed user input directly to Rhino's `eval()`, allowing arbitrary Java code execution on the device (e.g. `Runtime.getRuntime().exec(...)`). It also displayed "Infinity" for division by zero. | Replaced with a safe recursive-descent expression parser (`ExpressionEvaluator`). Every expression is tokenized and parsed as pure math; all code-injection attempts now throw a syntax error, division by zero throws a proper error, and floating-point noise is cleaned up (`0.1+0.2 = 0.3`). |
| 2 | Hardcoded, stale exchange rates for only 5 currencies. | Real-time rates from the ECB/Frankfurter API for 160+ currencies, with daily caching and an offline fallback. |
| 3 | No button keypad — calculation used a plain text field. | Complete calculator keypad layout with all standard buttons, symbols, scientific functions, memory, and history. |
| 4 | Missing unit conversion categories (no temperature, no full length/weight/volume sets) and unformatted results. | Full length, weight, volume, and temperature conversions; results formatted to up to 10 significant digits. |
| 5 | Missing `INTERNET` permission in the manifest. | Added to `AndroidManifest.xml`. |
| 6 | Unused Rhino dependency still declared. | Removed from `build.gradle.kts`. |

## Quality

The expression engine is covered by **18 unit tests** (`app/src/test/.../CalculatorLogicTest.kt`) covering arithmetic, operator precedence, powers, factorials, trigonometry in both angle modes, constants, implicit multiplication, error handling (division by zero, domain errors, invalid syntax), and code-injection blocking. Run them with:

```bash
./gradlew testDebugUnitTest
```

## Build

```bash
./gradlew assembleDebug
```

The APK lands at `app/build/outputs/apk/debug/app-debug.apk`.
