# Volume factor error

1 US cup = 236.5882365 mL = 0.0002365882365 L = 2.365882365e-7 m³.
I wrote 0.0002365882 — that is 236.588 L, i.e., 1000x a pint-size error... actually 0.0002365882 m³ = 236.5882 L?? No: 0.0002365882 m³ = 236.5882 L? 1 m³ = 1000 L, so 0.0002365882 m³ = 0.2365882 L = 236.5882 mL. That is correct! Wait — then gallon/cup = 0.0037854118 / 0.0002365882365 = 16.0?? Let me compute: 0.0037854118 / 0.0002365882365 = 16.0. Because 1 US gallon = 16 US CUPS? No! 1 gallon = 16 cups is TRUE in US customary (1 gallon = 4 quarts, 1 quart = 2 pints, 1 pint = 2 cups → 1 gal = 16 cups). My test expectation "8.0" was wrong! The correct answer is 16 cups.

# Conclusion: test expectation wrong, not the code.
- 1 US gallon = 16 US cups ✓ (customary). Fix test: expected 16.0.
