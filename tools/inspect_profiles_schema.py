import sqlite3

conn = sqlite3.connect(r'E:\MeComputer\Desktop\wosbot\database.db')
cursor = conn.cursor()
columns = cursor.execute("PRAGMA table_info(profiles);").fetchall()
print("Columns in profiles table:")
for col in columns:
    print(col)

rows = cursor.execute("SELECT * FROM profiles;").fetchall()
print("Rows in profiles:", rows)
conn.close()
