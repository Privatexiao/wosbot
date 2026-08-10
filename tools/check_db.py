import sqlite3

conn = sqlite3.connect(r'E:\MeComputer\Desktop\wosbot\database.db')
cursor = conn.cursor()
tables = [t[0] for t in cursor.execute("SELECT name FROM sqlite_master WHERE type='table';").fetchall()]
print("Tables:", tables)

for t in tables:
    count = cursor.execute(f"SELECT COUNT(*) FROM [{t}]").fetchone()[0]
    print(f"Table {t}: {count} rows")
    if "profile" in t.lower() or "account" in t.lower():
        rows = cursor.execute(f"SELECT * FROM [{t}]").fetchall()
        print(f"Content of {t}:", rows)

conn.close()
