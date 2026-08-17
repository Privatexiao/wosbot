import sqlite3
import re

enum_file = 'modules/api/src/main/java/dev/frostguard/api/configs/ConfigurationKeyEnum.java'
with open(enum_file, 'r', encoding='utf-8') as f:
    content = f.read()

enum_keys = set(re.findall(r'^\s*([A-Z0-9_]+)\s*\(', content, re.MULTILINE))

conn = sqlite3.connect('database.db')
cur = conn.cursor()
cols = [c[1] for c in cur.execute("PRAGMA table_info(config)").fetchall()]
print("Config columns:", cols)

db_keys = set([r[0] for r in cur.execute(f"SELECT DISTINCT config_key FROM config").fetchall()])
conn.close()

missing_keys = db_keys - enum_keys
print(f"Missing keys in ConfigurationKeyEnum.java ({len(missing_keys)}):")
for k in sorted(list(missing_keys)):
    print(" ", k)
