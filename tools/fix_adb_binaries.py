import shutil
import os
import urllib.request

ldplayer_dir = r"E:\LDeleumlnts\leidian\LDPlayer9"
tools_adb_dir = r"E:\MeComputer\Desktop\wosbot\tools\adb"
fg_app_adb_dir = r"E:\MeComputer\Desktop\wosbot\fg-app\target\lib\adb"

os.makedirs(tools_adb_dir, exist_ok=True)
os.makedirs(fg_app_adb_dir, exist_ok=True)

adb_files = ["adb.exe", "AdbWinApi.dll", "AdbWinUsbApi.dll"]

# 1. Try copying from LDPlayer9
copied_count = 0
for f in adb_files:
    src_path = os.path.join(ldplayer_dir, f)
    if os.path.exists(src_path) and os.path.getsize(src_path) > 10000:
        d1 = os.path.join(tools_adb_dir, f)
        d2 = os.path.join(fg_app_adb_dir, f)
        shutil.copy2(src_path, d1)
        shutil.copy2(src_path, d2)
        print(f"Copied {f} ({os.path.getsize(src_path)} bytes) from LDPlayer9 to tools/adb and fg-app/target/lib/adb")
        copied_count += 1

# 2. If LDPlayer files were not enough, download official Google Android Platform Tools
if copied_count < 3:
    print("Downloading Google Android Platform Tools...")
    zip_url = "https://dl.google.com/android/repository/platform-tools-latest-windows.zip"
    zip_path = os.path.join(tools_adb_dir, "adb.zip")
    urllib.request.urlretrieve(zip_url, zip_path)
    import zipfile
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        for member in zip_ref.namelist():
            if "adb.exe" in member or "AdbWinApi.dll" in member or "AdbWinUsbApi.dll" in member:
                filename = os.path.basename(member)
                if filename:
                    extracted_bytes = zip_ref.read(member)
                    with open(os.path.join(tools_adb_dir, filename), 'wb') as out_f:
                        out_f.write(extracted_bytes)
                    with open(os.path.join(fg_app_adb_dir, filename), 'wb') as out_f:
                        out_f.write(extracted_bytes)
                    print(f"Extracted {filename} from Google Platform Tools zip")
    os.remove(zip_path)

print("ADB binary fix completed successfully!")
