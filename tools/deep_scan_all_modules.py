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

# Regex to match strings in quotes inside FXML and Java
string_pattern = re.compile(r'"([^"\r\n]{2,80})"')

found_strings = set()

modules = ["fg-app", "fg-engine", "fg-tasks", "fg-api", "fg-vision"]

for module in modules:
    if os.path.exists(module):
        for root, dirs, files in os.walk(module):
            for file in files:
                if file.endswith(".fxml") or file.endswith(".java"):
                    path = os.path.join(root, file)
                    with open(path, "r", encoding="utf-8", errors="ignore") as f:
                        content = f.read()
                        matches = string_pattern.findall(content)
                        for m in matches:
                            m_clean = m.strip()
                            m_clean = m_clean.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                            # filter out code keywords, URLs, paths, regex, CSS styles, IDs
                            if (m_clean and 
                                not m_clean.startswith("http") and 
                                not m_clean.startswith("/") and 
                                not m_clean.startswith("-fx-") and 
                                not m_clean.startswith("mdi") and 
                                not m_clean.startswith("SELECT") and 
                                not m_clean.startswith("INSERT") and 
                                not m_clean.startswith("UPDATE") and 
                                not m_clean.endswith(".fxml") and 
                                not m_clean.endswith(".png") and 
                                not m_clean.endswith(".css") and 
                                not m_clean.endswith(".class") and 
                                not m_clean.endswith(".java") and 
                                not re.match(r'^[a-zA-Z0-9_\-\.\:\#\/\$\%\s]+\.java$', m_clean) and 
                                not re.match(r'^[0-9\.\:\s\%\#\-\_\$\{\}\?\<\>\,\;\!\@\^\*\(\)\/\\\'\"]+$', m_clean)):
                                # Ensure it has English letters and is readable text
                                if re.search(r'[a-zA-Z]{2,}', m_clean):
                                    found_strings.add(m_clean)

missing = [s for s in found_strings if s not in dictionary]
missing.sort()

out_missing = r"tools\deep_missing_strings.txt"
with open(out_missing, "w", encoding="utf-8") as f:
    f.write(f"Total candidate UI strings found: {len(found_strings)}\n")
    f.write(f"Total missing from dictionary: {len(missing)}\n\n")
    for s in missing:
        f.write(f"{s}\n")

print(f"Total candidate UI strings found across all modules: {len(found_strings)}")
print(f"Missing strings written to {out_missing}: {len(missing)}")
