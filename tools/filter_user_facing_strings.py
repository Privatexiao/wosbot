import os
import re

missing_file = r"tools\deep_missing_strings.txt"

user_facing = set()

ignore_exact = {
    "java", "javac", "maven", "target", "classes", "UTF-8", "INFO", "WARN", "ERROR", "DEBUG",
    "SUCCESS", "FAILED", "GET", "POST", "JSON", "PNG", "JPG", "JDBC", "WAL", "TRUE", "FALSE",
    "null", "true", "false", "void", "int", "boolean", "String", "Object", "List", "Set", "Map"
}

with open(missing_file, "r", encoding="utf-8") as f:
    for line in f:
        s = line.strip()
        if not s or s.startswith("Total "):
            continue
        
        # Check if it looks like a human readable UI string:
        # 1. Contains space AND starts with capital letter or punctuation
        # 2. Or is a short title word like 'Account', 'Settings', 'Profile', 'Emulator', etc.
        # 3. Does not contain java code characters like '.', '(', ')', ';', '{', '}', '='
        if any(c in s for c in ['(', ')', '{', '}', ';', '=', '<', '>', '/', '\\', '*', '$', '@']):
            continue
        
        if s in ignore_exact:
            continue

        # Check if contains spaces or looks like camel-case title words
        words = s.split()
        if len(words) >= 2:
            # Multi-word string, likely UI text or message
            if re.match(r'^[A-Z0-9\$\#\☕\🔴\🟢\❌\⏳\▶\📂\📝\🔍\🔙\🗑\👆\💡].*$', s) or re.match(r'^[a-z0-9].*$', s):
                user_facing.add(s)
        elif len(words) == 1:
            w = words[0]
            # Single word with capital first letter, e.g. "Actions", "Priority", "Status", "Emulator"
            if len(w) >= 3 and w[0].isupper() and not w.isupper():
                user_facing.add(w)

out_filtered = r"tools\user_facing_missing.txt"
missing_list = sorted(list(user_facing))

with open(out_filtered, "w", encoding="utf-8") as f:
    f.write(f"Total user facing missing strings: {len(missing_list)}\n\n")
    for s in missing_list:
        f.write(f"{s}\n")

print(f"Extracted {len(missing_list)} user-facing UI strings to {out_filtered}")
