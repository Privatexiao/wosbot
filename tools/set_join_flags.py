import sqlite3

db_paths = [
    r'E:\MeComputer\Desktop\wosbot\database.db',
    r'E:\MeComputer\Desktop\wosbot\fg-app\target\database.db'
]

for db in db_paths:
    conn = sqlite3.connect(db)
    cursor = conn.cursor()
    # Check if tp_config table exists
    tables = [t[0] for t in cursor.execute("SELECT name FROM sqlite_master WHERE type='table';").fetchall()]
    if 'tp_config' in tables:
        cursor.execute("UPDATE tp_config SET config_value = '1,2,3,4,5,6' WHERE config_key = 'BEAR_TRAP_JOIN_FLAG_INT';")
        if cursor.rowcount == 0:
            cursor.execute("INSERT INTO tp_config (profile_id, config_key, config_value) VALUES (1, 'BEAR_TRAP_JOIN_FLAG_INT', '1,2,3,4,5,6');")
        conn.commit()
        print(f"Updated BEAR_TRAP_JOIN_FLAG_INT to '1,2,3,4,5,6' in {db}")
    conn.close()
