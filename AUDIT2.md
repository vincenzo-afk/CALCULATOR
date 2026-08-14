# Phase 7-8 Audit — current app bugs & missing features

## UI/layout bugs
1. **Basic keypad row count mismatch**: memory row (mc m+ m− mr) uses 4 buttons but main rows use 4 (AC DEL ± ÷ etc.) — OK actually matches reference. BUT NumKey style is 4-col grid; basic keypad rows all 4-col = OK.
2. **Display text color**: in CALCULATOR mode displayResult coral — matches ref. In CURRENCY mode result view is conversionResult (grey) — OK.
3. **angleModeText color reset**: setMode resets icon colors but not angleModeText — fine.
4. **History panel eats layout**: historyPanel layout_weight=1 while display weight=1 — in history mode display gets half screen; acceptable but could shrink. Minor.
5. **Spinner theme on black**: default spinner popup is white bg — acceptable in Material; minor.
6. **TextWatcher double conversion loop**: convButtonClick calls displayInput.setText which fires TextWatcher → convertCurrency() again (idempotent, harmless but extra work) — fine.
7. **calcButtons wire only basic keypad buttons** — OK.
8. **wire() helper unused** (dead code) — remove.
9. **refreshDisplay called by buttons on basic keypad** but in currency mode TextWatcher also fires — harmless.

## Logic bugs
10. **2nd toggle labels don't change** — sin stays "sin" when 2nd pressed (ref shows asin). Fix: update button labels when 2nd toggled (sin→asin, cos→acos, tan→atan, x²→x³, 10ˣ→lg, eˣ→ln).
11. **DEG/RAD**: angleModeText starts "DEG" — ref starts "Rad" toggle key; fine. But DEG button in calcMap "DEG" token wired to nothing (only angleModeText click + btnSciRad handle it) — dead token; fine.
12. **Factorial "!"** wired in sci keypad? btnSciFact token "!" — onButtonClick handles it via else token → "!" appended; parser postfix handles. OK.
13. **Rand inserts value into expression** — OK but better to insert as number. Fine.
14. **EE inserts "e"** — conflicts: "exp(" is eˣ; EE should insert "e" as constant — but then "10e5" = 10*e*5 implicit mult = 135.9 fine. In currency mode "e" char won't matter (doubleOrNull fails). OK.
15. **percent button uses "%%" mapping "/(100)"** — 50% = 0.5 OK.
16. **lastAnswer floatArrayOf hack** — replace with class field. Cosmetic.
17. **addToHistory uses displayInput text AFTER '=' pressed** — text now replaced with result (input.setText(formatResult(...))) then addToHistory(text) uses the RESULT not expression → history shows "18=18" instead of "54−36=18"! BUG (line ~83-87). Fix: capture expr before setText.
18. **History entry with Error result** — adds noise; still OK, keep.
19. **spinnerConversionType removed** — unit converter (length/weight/volume/temp/area) NO LONGER ACCESSIBLE — the converter panel is currency-only now! MISSING FEATURE: user asked for "all the calculations" — need category switching in converter panel. Add category spinner or icon toggle in converter panel.
20. **CurrencyRepository.convert returns null** when units unknown — fine.
21. **getRates URL** had stale base param (fixed earlier). Fine.

## Missing features to add
- Unit converter categories: Currency, Length, Weight, Volume, Temperature + Area, Data (MB/GB), Time (sec/min/hr) — add via category selector bar in converter panel.
- 2nd toggle label swap.
- History shows real expression (fix #17).

## Plan
1. Fix #17 history bug, #21 (none left), remove dead wire() helper, class-field lastAnswer.
2. 2nd toggle label swap (sin↔asin etc. pairs).
3. Add converter categories: selector row in converterPanel with spinners shared (fromUnit/toUnit) + unit sets; convert on selection change; keep currency live.
4. Add Area, Data, Time conversions.
5. Extend tests for new conversions + history expression bug not unit-testable (UI). Add parser tests for new functions (asinh etc already).
6. Build, commit, push, deliver.
