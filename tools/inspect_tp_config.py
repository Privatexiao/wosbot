import sqlite3

conn = sqlite3.connect(r'E:\MeComputer\Desktop\wosbot\database.db')
cursor = conn.cursor()
cols = cursor.execute("PRAGMA table_info(config);").fetchall()
print("config columns:")
for c in cols:
    print(c)

rows = cursor.execute("SELECT * FROM config WHERE config_key LIKE '%BEAR%' OR config_key LIKE '%FLAG%';").fetchall()
print("BEAR/FLAG rows:", rows)
conn.close()
