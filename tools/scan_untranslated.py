import os
import re

properties_file = r"fg-app\src\main\resources\i18n\messages_zh_CN.properties"

dictionary = {}
if os.path.exists(properties_file):
    with open(properties_file, "r", encoding="utf-8") as f:
        for line in f:
            if "=" in line and not line.strip().startswith("#"):
                k, v = line.split("=", 1)
                dictionary[k.strip()] = v.strip()

print(f"Current dictionary size: {len(dictionary)}")

text_pattern = re.compile(r'(?:text|promptText|title|headerText)\s*=\s*"([^"]+)"')

found_texts = set()

for root, dirs, files in os.walk("fg-app"):
    for file in files:
        if file.endswith(".fxml") or file.endswith(".java"):
            path = os.path.join(root, file)
            with open(path, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
                matches = text_pattern.findall(content)
                for m in matches:
                    m_clean = m.strip()
                    if m_clean and not m_clean.startswith("%") and not m_clean.startswith("${"):
                        # ignore pure numbers / symbols / empty
                        if not re.match(r'^[0-9\.\:\s\%\#\-\_\$\{\}\?\<\>\,\;\!\@\^\*\(\)\/\\]+$', m_clean):
                            found_texts.add(m_clean)

missing = [t for t in found_texts if t not in dictionary]
missing.sort()

print(f"\nTotal unique UI texts found: {len(found_texts)}")
print(f"Missing from dictionary: {len(missing)}\n")

for m in missing:
    print(f"MISSING: {m}")
