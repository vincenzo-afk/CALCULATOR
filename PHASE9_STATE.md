# Phase 9 state — remaining fixes

## Current compile errors (MainActivity.kt)
1. Line 253: Unresolved reference 'refreshConverter' — called inside setConverterCategory BEFORE its declaration. Kotlin local funs are NOT hoisted. FIX: move `fun refreshConverter()` definition BEFORE `fun setConverterCategory()` (around line 224).
2. Line 291: forEach with Button lambda type inference fail — in categoryRow.forEach loop: `categoryRow.forEach { child -> if (child is Button) child.setTextColor(0xFF999999.toInt()) }` — child is View? (LinearLayout children are View). Fix: `for (i in 0 until categoryRow.childCount) { val c = categoryRow.getChildAt(i); if (c is Button) c.setTextColor(0xFF999999.toInt()) }`.

## Done so far (not to redo)
- History bug fixed: addToHistory called with original expression text (before setText replaced it) — verified in '=' branch at line ~72-83 (addToHistory(text, result) before input.setText(result)).
- 2nd toggle now swaps labels (asin/acos/atan, x³, lg, ln) AND the trig/cube/power/exp keys use secondFunction flag in onButtonClick (tokens "sin(","x²","10ˣ","eˣ" map via when with secondFunction).
- Removed dead wire() helper; lastAnswer class field.
- UnitConverter.kt created with LENGTH/WEIGHT/VOLUME/AREA/DATA/TIME/TEMPERATURE; categories(): Currency, Length, Weight, Volume, Area, Data, Time, Temperature.
- Converter panel in layout has converterCategoryRow (HorizontalScrollView) + category chips created in code; spinner adapters use R.layout.spinner_item_dark.
- setConverterCategory("Currency") called as initial setup; refreshConverter dispatches currency vs units; convButtonClick/TextWatcher call refreshConverter.
- UnitConverterTest.kt added (9 tests).
- spinner_item_dark.xml layout created.

## Build commands
cd /home/ubuntu/calculator && export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ANDROID_HOME=~/android-sdk
tests: ./gradlew testDebugUnitTest --no-daemon
apk: ./gradlew assembleDebug --no-daemon && cp app/build/outputs/apk/debug/app-debug.apk /home/ubuntu/Calculator-debug.apk
push: git -c user.name="vincenzo-afk" -c user.email="itsmebk2007@gmail.com" add -A; commit; git push origin main
(Avoid commit messages with 'x!' text — bash history expansion issue; use single-quoted commit message or write message to file with git commit -F msg.txt)

## After push: deliver result message with /home/ubuntu/Calculator-debug.apk attached + summary of fixes.
## Summary for delivery: history expression bug, 2nd-toggle labels+behavior, category converter (8 categories: currency + 7 unit types), dark spinner theme, removed dead code, new UnitConverter tests; all tests pass.
