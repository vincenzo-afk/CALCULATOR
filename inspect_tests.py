import re
from pathlib import Path

results = Path('/home/ubuntu/calculator/app/build/test-results/testDebugUnitTest/')
for f in sorted(results.glob('TEST-*.xml')):
    data = f.read_text(encoding='utf-8')
    if '<failure' not in data and 'failures="0"' in data:
        continue
    print('==', f.name)
    for m in re.finditer(r'<testcase classname="[^"]*" name="([^"]+)"[^>]*/?>(?:.*?</testcase>)?', data, re.S):
        block = m.group(0)
        if '<failure' in block:
            msg = re.search(r'<failure message="([^"]*)"', block)
            print('  FAIL:', m.group(1))
            print('   ', (msg.group(1) if msg else '?')[:400])
