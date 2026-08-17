import sqlite3
import os

print("=== Checking database files ===")
for path in ['database.db', '.frostguard-dev/frostguard.db']:
    if os.path.exists(path):
        conn = sqlite3.connect(path)
        cursor = conn.cursor()
        tables = [t[0] for t in cursor.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
        print(f"\nDatabase {path} (size: {os.path.getsize(path)} bytes):")
        print("  Tables:", len(tables), tables)
        for t in ['profile', 'profiles', 'setting', 'settings', 'config', 'configs']:
            if t in tables:
                rows = cursor.execute(f"SELECT count(*) FROM {t}").fetchone()[0]
                print(f"  Table '{t}' count: {rows}")
                if rows > 0 and rows < 20:
                    data = cursor.execute(f"SELECT * FROM {t} LIMIT 10").fetchall()
                    print(f"  Sample {t} data:", data)
