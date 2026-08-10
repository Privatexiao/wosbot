import sqlite3
import shutil
import os

root_db = r'E:\MeComputer\Desktop\wosbot\database.db'
target_db = r'E:\MeComputer\Desktop\wosbot\fg-app\target\database.db'

# Ensure root_db has account 'lin' with emulator 2
conn = sqlite3.connect(root_db)
cursor = conn.cursor()

# Check profiles
profiles = cursor.execute("SELECT id, name, emulator_number FROM profiles;").fetchall()
print("Existing profiles in root DB:", profiles)

# Rename 'Default' profile to 'lin' and set emulator_number to '2' if it's default
has_lin = any(p[1] == 'lin' for p in profiles)
if not has_lin:
    cursor.execute("UPDATE profiles SET name = 'lin', emulator_number = '2' WHERE name = 'Default' OR id = 1;")
    if cursor.rowcount == 0:
        cursor.execute("INSERT INTO profiles (id, enabled, emulator_number, name, priority) VALUES (1, 1, '2', 'lin', 50);")
    conn.commit()
    print("Updated/Inserted profile 'lin' (Emulator 2) into root database.")

profiles_after = cursor.execute("SELECT id, name, emulator_number FROM profiles;").fetchall()
print("Profiles after update:", profiles_after)
conn.close()

# Copy updated root_db to target_db
os.makedirs(os.path.dirname(target_db), exist_ok=True)
shutil.copy2(root_db, target_db)
print(f"Synced {root_db} to {target_db}")
