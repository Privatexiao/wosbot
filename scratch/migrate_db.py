import sqlite3
import os
import shutil

src_db = 'database.db'
dst_db = '.frostguard-dev/frostguard.db'

print(f"Migrating from {src_db} to {dst_db}...")

if not os.path.exists(src_db):
    print("Source database.db not found!")
    exit(1)

if os.path.exists(dst_db):
    shutil.copyfile(dst_db, dst_db + '.bak')

conn_src = sqlite3.connect(src_db)
conn_dst = sqlite3.connect(dst_db)

cur_src = conn_src.cursor()
cur_dst = conn_dst.cursor()

tables_src = [t[0] for t in cur_src.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall() if not t[0].startswith('sqlite_')]

for table in tables_src:
    rows = cur_src.execute(f"SELECT * FROM {table}").fetchall()
    if not rows:
        continue
    cols_src = [c[1] for c in cur_src.execute(f"PRAGMA table_info({table})").fetchall()]
    
    dst_tables = [t[0] for t in cur_dst.execute("SELECT name FROM sqlite_master WHERE type='table'").fetchall()]
    if table not in dst_tables:
        print(f"Table '{table}' missing in destination, skipping...")
        continue
    
    cols_dst = [c[1] for c in cur_dst.execute(f"PRAGMA table_info({table})").fetchall()]
    common_cols = [c for c in cols_src if c in cols_dst]
    
    if not common_cols:
        continue
        
    cols_str = ", ".join(common_cols)
    placeholders = ", ".join(["?"] * len(common_cols))
    
    cur_dst.execute(f"DELETE FROM {table}")
    
    src_data = cur_src.execute(f"SELECT {cols_str} FROM {table}").fetchall()
    cur_dst.executemany(f"INSERT INTO {table} ({cols_str}) VALUES ({placeholders})", src_data)
    print(f"Migrated {len(src_data)} rows into table '{table}'")

conn_dst.commit()
conn_src.close()
conn_dst.close()

print("Database migration completed successfully!")
