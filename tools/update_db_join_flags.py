import sqlite3
import shutil
import os

db_paths = [
    r'E:\MeComputer\Desktop\wosbot\database.db',
    r'E:\MeComputer\Desktop\wosbot\fg-app\target\database.db'
]

for db in db_paths:
    if os.path.exists(db):
        conn = sqlite3.connect(db)
        cursor = conn.cursor()
        cursor.execute("UPDATE config SET value = '1,2,3,4,5,6' WHERE config_key = 'BEAR_TRAP_JOIN_FLAG_INT';")
        conn.commit()
        print(f"Updated BEAR_TRAP_JOIN_FLAG_INT to '1,2,3,4,5,6' in {db} (updated {cursor.rowcount} rows)")
        conn.close()

root_db = r'E:\MeComputer\Desktop\wosbot\database.db'
target_db = r'E:\MeComputer\Desktop\wosbot\fg-app\target\database.db'
for ext in ['', '-shm', '-wal']:
    if os.path.exists(root_db + ext):
        shutil.copy2(root_db + ext, target_db + ext)
        print(f"Synced {root_db + ext} to {target_db + ext}")
