import sqlite3
import shutil
import os

root_db = r'E:\MeComputer\Desktop\wosbot\database.db'
target_db = r'E:\MeComputer\Desktop\wosbot\fg-app\target\database.db'

conn = sqlite3.connect(root_db)
cursor = conn.cursor()

# Update profile_name to 'lin' and emulator_number to '2'
cursor.execute("UPDATE profiles SET profile_name = 'lin', emulator_number = '2' WHERE id = 1;")
conn.commit()

# Check updated profiles
profiles = cursor.execute("SELECT id, profile_name, emulator_number FROM profiles;").fetchall()
print("Updated profiles in root database:", profiles)

conn.close()

# Sync root_db to target_db and clear WAL temp files if present
os.makedirs(os.path.dirname(target_db), exist_ok=True)
shutil.copy2(root_db, target_db)

# Check if target_db has WAL files, copy WAL too
for ext in ['-shm', '-wal']:
    f_root = root_db + ext
    f_target = target_db + ext
    if os.path.exists(f_root):
        shutil.copy2(f_root, f_target)
        print(f"Synced {f_root} to {f_target}")

print("Profile 'lin' (Emulator: 2) restored successfully in both locations!")
