import shutil
import os

src = r"E:\MeComputer\Desktop\wosbot\tools\tesseract"
dest1 = r"E:\MeComputer\Desktop\wosbot\fg-app\target\lib\tesseract"
dest2 = r"E:\MeComputer\Desktop\wosbot\fg-app\src\main\resources\tesseract"

os.makedirs(dest1, exist_ok=True)
os.makedirs(dest2, exist_ok=True)

for item in os.listdir(src):
    s = os.path.join(src, item)
    if os.path.isfile(s):
        d1 = os.path.join(dest1, item)
        d2 = os.path.join(dest2, item)
        shutil.copy2(s, d1)
        shutil.copy2(s, d2)
        print(f"Copied {item} ({os.path.getsize(s)} bytes) to dest1 & dest2")

print("Copy completed successfully!")
