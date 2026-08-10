import sqlite3
import os

db_paths = [
    r'E:\MeComputer\Desktop\wosbot\database.db',
    r'E:\MeComputer\Desktop\wosbot\fg-app\target\database.db'
]

for path in db_paths:
    if os.path.exists(path):
        print(f"=== DB: {path} (size: {os.path.getsize(path)} bytes) ===")
        conn = sqlite3.connect(path)
        cursor = conn.cursor()
        try:
            profiles = cursor.execute("SELECT * FROM profiles;").fetchall()
            print("Profiles:", profiles)
        except Exception as e:
            print("Error querying profiles:", e)
        conn.close()
